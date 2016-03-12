package com.karasiq.nanoboard.dispatcher

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.karasiq.nanoboard.model._
import com.karasiq.nanoboard.server.streaming.NanoboardEvent
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage, NanoboardMessageGenerator}
import com.typesafe.config.{Config, ConfigFactory}
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object NanoboardSlickDispatcher {
  def apply(db: Database, config: Config = ConfigFactory.load(), eventSink: Sink[NanoboardEvent, _] = Sink.ignore)(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer): NanoboardDispatcher = {
    new NanoboardSlickDispatcher(db, config, eventSink)
  }
}

private[dispatcher] final class NanoboardSlickDispatcher(db: Database, config: Config, eventSink: Sink[NanoboardEvent, _])(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer) extends NanoboardDispatcher {
  private val messageGenerator = NanoboardMessageGenerator.fromConfig(config)
  private val eventQueue = Source.queue(20, OverflowStrategy.dropHead)
    .to(eventSink)
    .run()

  override def createContainer(pendingCount: Int, randomCount: Int, format: String, container: ByteString) = {
    val pending = Post.pending(0, pendingCount)
    val rand = SimpleFunction.nullary[Double]("rand")
    val random = posts.sortBy(_ ⇒ rand).take(randomCount).result.map(_.map(_.asThread(0)))
    val query = for {
      p ← pending
      r ← random
    } yield Random.shuffle((p ++ r).toVector)

    val stage = Seq(GzipCompression(), SalsaCipher.fromConfig(config), PngEncoding(data ⇒ {
      val inputStream = new ByteArrayInputStream(container.toArray)
      val image = try { ImageIO.read(inputStream) } finally inputStream.close()
      assert(image.ne(null), "Invalid image")
      assert((image.getWidth * image.getHeight * 3) >= data.length, s"Image is too small, ${data.length} bytes required")
      image
    }))

    val future = db.run(query).map { posts ⇒
      val data: ByteString = ByteString(NanoboardMessage.writeMessages(posts.map(m ⇒ NanoboardMessage(m.parent.get, m.text))))
      val encoded = stage.encode(data)
      assert(stage.decode(encoded) == data, "Container is broken")
      posts.map(_.hash) → encoded
    }

    future.flatMap {
      case (posts, result) ⇒
        db.run(pendingPosts.filter(_.hash inSet posts).delete)
          .map(_ ⇒ result)
    }
  }

  override def recent(offset: Int, count: Int): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.recent(offset, count))
  }

  def pending(offset: Int, count: Int): Future[Seq[NanoboardMessageData]] = {
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

  override def thread(hash: String, offset: Int, count: Int): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.thread(hash, offset, count))
  }

  override def markAsNotPending(message: String): Future[Unit] = {
    db.run(DBIO.seq(pendingPosts.filter(_.hash === message).delete))
  }

  override def markAsPending(message: String): Future[Unit] = {
    db.run(DBIO.seq(pendingPosts.insertOrUpdate(message)))
  }

  override def delete(hash: String): Future[Seq[String]] = {
    val future = db.run(Post.delete(hash))
    future.foreach { deleted ⇒
      deleted.foreach(hash ⇒ eventQueue.offer(NanoboardEvent.PostDeleted(hash)))
    }
    future
  }

  override def delete(offset: Int, count: Int): Future[Seq[String]] = {
    val query = for {
      ps ← posts.sortBy(_.firstSeen.desc).drop(offset).take(count).result
      deleted ← DBIO.sequence(ps.map(p ⇒ Post.delete(p.hash)))
    } yield deleted.flatten

    val future = db.run(query)
    future.foreach { deleted ⇒
      deleted.foreach(hash ⇒ eventQueue.offer(NanoboardEvent.PostDeleted(hash)))
    }
    future
  }

  override def addPost(message: NanoboardMessage): Future[Int] = {
    db.run(Post.insertMessage(message))
  }

  override def reply(parent: String, text: String): Future[NanoboardMessageData] = {
    val newMessage: NanoboardMessage = messageGenerator.newMessage(parent, text)
    eventQueue.offer(NanoboardEvent.PostAdded(newMessage, true))
    db.run(Post.addReply(newMessage)).map(_ ⇒ NanoboardMessageData(Some(parent), newMessage.hash, newMessage.text, 0))
  }

  override def updatePlaces(places: Seq[String]): Future[Unit] = {
    db.run(Place.update(places))
  }

  override def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit] = {
    db.run(Category.update(categories))
  }
}
