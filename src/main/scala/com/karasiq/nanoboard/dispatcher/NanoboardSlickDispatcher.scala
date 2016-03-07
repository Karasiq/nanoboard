package com.karasiq.nanoboard.dispatcher

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.karasiq.nanoboard.server.model._
import com.karasiq.nanoboard.sources.bitmessage.BitMessageTransport
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
import com.typesafe.config.Config
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

final class NanoboardSlickDispatcher(config: Config, db: Database)(implicit ec: ExecutionContext, as: ActorSystem, am: ActorMaterializer) extends NanoboardDispatcher {
  private val encryptionKey = config.getString("nanoboard.encryption-key")

  private val log = Logging(as, "NanoboardDispatcher")

  private val bitMessage = new BitMessageTransport(config, { message ⇒
    log.debug("Message from BM transport received: {}", message)
    db.run(Post.insertMessage(message))
  })

  Http().bindAndHandle(bitMessage.route, "127.0.0.1", config.getInt("nanoboard.bitmessage.listen-port"))

  override def createContainer(pendingCount: Int, randomCount: Int, format: String, container: ByteString) = {
    val pending = Post.pending(0, pendingCount)
    val rand = SimpleFunction.nullary[Double]("rand")
    val random = posts.sortBy(_ ⇒ rand).take(randomCount).result.map(_.map(_.asThread(0)))
    val query = for {
      p ← pending
      r ← random
    } yield Random.shuffle((p ++ r).toVector)

    val stage = Seq(GzipCompression(), SalsaCipher(encryptionKey), PngEncoding(data ⇒ {
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

  override def delete(message: String): Future[Unit] = {
    db.run(Post.delete(message))
  }

  override def reply(parent: String, text: String): Future[NanoboardMessageData] = {
    val newMessage: NanoboardMessage = NanoboardMessage.newMessage(parent, text)
    if (config.getBoolean("nanoboard.bitmessage.send")) {
      bitMessage.sendMessage(newMessage).foreach { response ⇒
        log.info("Message sent to BM transport: {}", response)
      }
    }
    db.run(Post.addReply(newMessage)).map(_ ⇒ NanoboardMessageData(Some(parent), newMessage.hash, newMessage.text, 0))
  }

  override def updatePlaces(places: Seq[String]): Future[Unit] = {
    db.run(Place.update(places))
  }

  override def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit] = {
    db.run(Category.update(categories))
  }
}
