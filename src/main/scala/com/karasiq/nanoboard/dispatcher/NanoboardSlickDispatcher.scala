package com.karasiq.nanoboard.dispatcher

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.karasiq.nanoboard.api.{NanoboardCaptchaImage, NanoboardCaptchaRequest, NanoboardContainer, NanoboardMessageData}
import com.karasiq.nanoboard.captcha.storage.NanoboardCaptchaSource
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardPow}
import com.karasiq.nanoboard.encoding.NanoboardEncoding
import com.karasiq.nanoboard.encoding.stages.PngEncoding
import com.karasiq.nanoboard.model.MessageConversions._
import com.karasiq.nanoboard.model.{categories => categoriesTable, _}
import com.karasiq.nanoboard.streaming.NanoboardEvent
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage, NanoboardMessageGenerator}
import com.typesafe.config.{Config, ConfigFactory}
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}

object NanoboardSlickDispatcher {
  def apply(db: Database, captcha: NanoboardCaptchaSource, config: Config = ConfigFactory.load(), eventSink: Sink[NanoboardEvent, _] = Sink.ignore)(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer): NanoboardDispatcher = {
    new NanoboardSlickDispatcher(db, captcha, config, eventSink)
  }
}

private[dispatcher] final class NanoboardSlickDispatcher(db: Database, captcha: NanoboardCaptchaSource, config: Config, eventSink: Sink[NanoboardEvent, _])(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer) extends NanoboardDispatcher {
  private val powContext = NanoboardPow.executionContext()
  private val powCalculator = NanoboardPow(config)(powContext)
  private val messageGenerator = NanoboardMessageGenerator(config)
  private val eventQueue = Source.queue(20, OverflowStrategy.dropHead)
    .to(eventSink)
    .run()

  override def createContainer(pendingCount: Int, randomCount: Int, format: String, container: ByteString) = {
    val pending = Post.pending(0, pendingCount)
    val rand = SimpleFunction.nullary[Double]("rand")
    val random = for (ps ← posts.sortBy(_ ⇒ rand).take(randomCount).result) yield ps.map(MessageConversions.wrapDbPost(_, 0))
    val query = for (p ← pending; r ← random) yield (p ++ r).toVector

    val stage = NanoboardEncoding(config, PngEncoding.fromEncodedImage(container))
    val future = db.run(query).map { posts ⇒
      val data = NanoboardMessage.writeMessages(posts.map(MessageConversions.unwrapToMessage))
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
        if (inserted > 0) eventQueue.offer(NanoboardEvent.PostAdded(wrapMessage(message, Some(container))))
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
    val query = for {
      (parent, text) ← posts.filter(_.hash === hash).map(p ⇒ (p.parent, p.text)).result.head
      powPayload ← DBIO.successful(NanoboardPow.dataToPow(NanoboardMessage(parent, text)))
      pow ← DBIO.from(powCalculator.calculate(powPayload))
      captchaId ← DBIO.successful(NanoboardCaptcha.index(powPayload ++ pow, captcha.length))
      captchaImage ← DBIO.from(captcha(captchaId))
    } yield NanoboardCaptchaRequest(hash, pow.toArray, NanoboardCaptchaImage(captchaId, NanoboardCaptcha.render(captchaImage).toArray))
    db.run(query)
  }

  def verifyPost(request: NanoboardCaptchaRequest, answer: String): Future[NanoboardMessageData] = {
    val query = for {
      DBPost(_, parent, text, firstSeen, containerId, _, _) ← posts.filter(_.hash === request.postHash).result.head
      pow ← DBIO.successful(ByteString(request.pow))
      unsigned ← DBIO.successful(NanoboardCaptcha.dataToSign(NanoboardMessage(parent, text, pow)))
      captchaId ← DBIO.successful(NanoboardCaptcha.index(unsigned, captcha.length))
      captcha ← DBIO.from(this.captcha(captchaId))
      signature ← DBIO.successful(captcha.signature(unsigned, answer)) if captcha.verify(unsigned, signature)
      newMessage ← DBIO.successful(NanoboardMessage(parent, text, pow, signature))
      newPost ← DBIO.seq(
        posts.filter(_.hash === request.postHash).delete,
        posts += DBPost(newMessage.hash, newMessage.parent, newMessage.text, firstSeen, containerId, pow, signature),
        pendingPosts += newMessage.hash
      )
    } yield MessageConversions.wrapMessage(newMessage, Some(containerId))
    db.run(query)
  }
}
