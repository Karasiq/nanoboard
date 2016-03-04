package com.karasiq.nanoboard.dispatcher

import com.karasiq.nanoboard.server.model._
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}

final class NanoboardSlickDispatcher(db: Database)(implicit ec: ExecutionContext) extends NanoboardDispatcher {
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
