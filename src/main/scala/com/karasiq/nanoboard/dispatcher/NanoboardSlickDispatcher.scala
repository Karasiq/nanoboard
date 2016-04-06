package com.karasiq.nanoboard.dispatcher

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.karasiq.nanoboard.api.{NanoboardCaptchaImage, NanoboardCaptchaRequest, NanoboardContainer, NanoboardMessageData}
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardCaptchaFile, NanoboardPow}
import com.karasiq.nanoboard.encoding.NanoboardEncoding
import com.karasiq.nanoboard.encoding.stages.PngEncoding
import com.karasiq.nanoboard.model.MessageConversions._
import com.karasiq.nanoboard.model._
import com.karasiq.nanoboard.streaming.NanoboardEvent
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage, NanoboardMessageGenerator}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.binary.Hex
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}

object NanoboardSlickDispatcher {
  def apply(db: Database, captcha: NanoboardCaptchaFile, config: Config = ConfigFactory.load(), eventSink: Sink[NanoboardEvent, _] = Sink.ignore)(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer): NanoboardDispatcher = {
    new NanoboardSlickDispatcher(db, captcha, config, eventSink)
  }
}

private[dispatcher] final class NanoboardSlickDispatcher(db: Database, captcha: NanoboardCaptchaFile, config: Config, eventSink: Sink[NanoboardEvent, _])(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer) extends NanoboardDispatcher {
  private val powCalculator = NanoboardPow(config)
  private val messageGenerator = NanoboardMessageGenerator(config)
  private val eventQueue = Source.queue(20, OverflowStrategy.dropHead)
    .to(eventSink)
    .run()

  override def createContainer(pendingCount: Int, randomCount: Int, format: String, container: ByteString) = {
    val pending = Post.pending(0, pendingCount)
    val rand = SimpleFunction.nullary[Double]("rand")
    val random = for (ps ← posts.sortBy(_ ⇒ rand).take(randomCount).result) yield ps.map(_.asThread(0))
    val query = for (p ← pending; r ← random) yield (p ++ r).toVector

    val stage = NanoboardEncoding(config, PngEncoding { data ⇒
      val inputStream = new ByteArrayInputStream(container.toArray)
      val image = try { ImageIO.read(inputStream) } finally inputStream.close()
      assert(image.ne(null), "Invalid image")
      assert(PngEncoding.imageBytes(image) >= data.length, s"Image is too small, ${data.length} bits required")
      image
    })

    val future = db.run(query).map { posts ⇒
      val data = ByteString(NanoboardMessage.writeMessages(posts.map(messageDataToMessage)))
      val encoded = stage.encode(data)
      // assert(stage.decode(encoded) == data, "Container is broken")
      posts.map(_.hash) → encoded
    }

    future.flatMap {
      case (hashes, result) ⇒
        db.run(for (_ ← pendingPosts.filter(_.hash inSet hashes).delete) yield result)
    }
  }

  override def recent(offset: Long, count: Long): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.recent(offset, count))
  }

  override def pending(offset: Long, count: Long): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.pending(offset, count))
  }

  override def places(): Future[Seq[String]] = {
    db.run(Place.list())
  }

  override def categories(): Future[Seq[NanoboardMessageData]] = {
    db.run(Category.list())
  }

  override def post(hash: String): Future[Option[NanoboardMessageData]] = {
    db.run(Post.get(hash))
  }

  override def thread(hash: String, offset: Long, count: Long): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.thread(hash, offset, count))
  }

  override def markAsNotPending(message: String): Future[Unit] = {
    db.run(DBIO.seq(pendingPosts.filter(_.hash === message).delete))
  }

  override def markAsPending(message: String): Future[Unit] = {
    db.run(DBIO.seq(pendingPosts.insertOrUpdate(message)))
  }

  override def delete(hash: String): Future[Seq[String]] = {
    val future = db.run(Post.deleteCascade(hash))
    future.foreach { deleted ⇒
      deleted.foreach(hash ⇒ eventQueue.offer(NanoboardEvent.PostDeleted(hash)))
    }
    future
  }

  override def delete(offset: Long, count: Long): Future[Seq[String]] = {
    val query = for {
      ps ← posts.sortBy(_.firstSeen.desc).drop(offset).take(count).result
      deleted ← DBIO.sequence(ps.map(p ⇒ Post.delete(p.hash)))
    } yield deleted

    val future = db.run(query)
    future.foreach { deleted ⇒
      deleted.foreach(hash ⇒ eventQueue.offer(NanoboardEvent.PostDeleted(hash)))
    }
    future
  }

  override def clearDeleted(): Future[Int] = {
    db.run(deletedPosts.delete)
  }

  override def addPost(source: String, message: NanoboardMessage): Future[Int] = {
    val query = for {
      container ← Container.forUrl(source)
      inserted ← Post.insertMessage(container, message)
    } yield (container, inserted)

    val future = db.run(query)
    future.foreach {
      case (container, inserted) ⇒
        if (inserted > 0) eventQueue.offer(NanoboardEvent.PostAdded(asMessageData(message, Some(container))))
    }
    future.map(_._2)
  }

  override def reply(parent: String, text: String): Future[NanoboardMessageData] = {
    val newMessage: NanoboardMessage = messageGenerator.newMessage(parent, text)
    val future = db.run(Post.addReply(newMessage))
    future.foreach { msg ⇒
      eventQueue.offer(NanoboardEvent.PostAdded(msg, pending = true))
    }
    future
  }

  override def updatePlaces(places: Seq[String]): Future[Unit] = {
    db.run(Place.update(places))
  }

  override def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit] = {
    db.run(Category.update(categories))
  }

  override def containers(offset: Long, count: Long): Future[Seq[NanoboardContainer]] = {
    db.run(Container.list(offset, count))
  }

  override def clearContainer(id: Long): Future[Seq[String]] = {
    db.run(Container.clearPosts(id))
  }

  def requestVerification(hash: String): Future[NanoboardCaptchaRequest] = {
    for {
      post ← db.run(Post.get(hash)) if post.nonEmpty && !post.exists(p ⇒ p.answers > 0 || p.text.contains("[sign="))
      pow ← powCalculator.calculate(ByteString(post.get.text))
      captchaId ← Future.successful(powCalculator.captchaIndex(ByteString(post.get.text) ++ pow, captcha.length))
      captchaImage ← captcha(captchaId)
    } yield NanoboardCaptchaRequest(hash, pow.utf8String, NanoboardCaptchaImage(captchaId, NanoboardCaptcha.render(captchaImage).toArray))
  }

  def verifyPost(request: NanoboardCaptchaRequest, answer: String): Future[NanoboardMessageData] = {
    for {
      DBPost(_, parent, text, firstSeen, cid) ← db.run(posts.filter(_.hash === request.post).result.head) if !text.contains("[sign=")
      captchaId ← Future.successful(powCalculator.captchaIndex(ByteString(text + request.pow), captcha.length))
      captcha ← this.captcha(captchaId)
      sign ← Future.successful(captcha.signature(ByteString(text + request.pow), answer)) if captcha.verify(ByteString(text + request.pow), sign)
      newMessage ← Future.successful(NanoboardMessage(parent, text + request.pow + s"[sign=${Hex.encodeHexString(sign.toArray)}]"))
      newPost ← db.run(DBIO.seq(
        posts.filter(_.hash === request.post).delete,
        posts += DBPost(newMessage.hash, newMessage.parent, newMessage.text, firstSeen, cid),
        pendingPosts += newMessage.hash
      ))
    } yield NanoboardMessageData(Some(cid), Some(parent), newMessage.hash, newMessage.text, 0)
  }
}
