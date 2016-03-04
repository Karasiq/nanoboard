package com.karasiq.nanoboard.dispatcher

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.karasiq.nanoboard.server.model._
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
import com.typesafe.config.Config
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

final class NanoboardSlickDispatcher(config: Config, db: Database)(implicit ec: ExecutionContext) extends NanoboardDispatcher {
  private val encryptionKey = config.getString("nanoboard.encryption-key")

  override def createContainer(pendingCount: Int, randomCount: Int, format: String, container: ByteString) = {
    val pending = Post.pending().take(pendingCount)
    val rand = SimpleFunction.nullary[Double]("rand")
    val random = posts.sortBy(_ ⇒ rand).take(randomCount)
    val query = for {
      p ← pending.result
      r ← random.result
    } yield Random.shuffle((p ++ r).toVector).map { post ⇒
      NanoboardMessageData(Some(post.parent), post.hash, post.message, 0)
    }

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

  def pending(): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.pending().map(p ⇒ p.parent → p.message).result).map(_.map {
      case (parent, text) ⇒
        NanoboardMessageData(Some(parent), NanoboardMessage(parent, text).hash, text, 0)
    })
  }

  override def places(): Future[Seq[String]] = {
    db.run(Place.list())
  }

  override def categories(): Future[Seq[NanoboardMessageData]] = {
    db.run(Category.list()).map(_.map {
      case (NanoboardCategory(hash, name), answers) ⇒
        NanoboardMessageData(None, hash, name, answers)
    })
  }

  override def get(thread: String, offset: Int, count: Int): Future[Seq[NanoboardMessageData]] = {
    db.run(Post.thread(thread, offset, count))
      .map(_.map {
        case (m @ NanoboardMessage(parent, text), answers) ⇒
          NanoboardMessageData(Some(parent), m.hash, text, answers)
      })
  }

  override def delete(message: String): Future[Unit] = {
    db.run(Post.delete(message))
  }

  override def reply(parent: String, text: String): Future[NanoboardMessageData] = {
    val newMessage: NanoboardMessage = NanoboardMessage.newMessage(parent, text)
    db.run(Post.addReply(newMessage)).map(_ ⇒ NanoboardMessageData(Some(parent), newMessage.hash, newMessage.text, 0))
  }

  override def updatePlaces(places: Seq[String]): Future[Unit] = {
    db.run(Place.update(places))
  }

  override def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit] = {
    db.run(Category.update(categories))
  }
}
