package com.karasiq.nanoboard.dispatcher

import com.karasiq.nanoboard.server.model._
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
import slick.driver.H2Driver.api._

import scala.concurrent.{ExecutionContext, Future}

final class NanoboardSlickDispatcher(db: Database)(implicit ec: ExecutionContext) extends NanoboardDispatcher {
  override def categories(): Future[Seq[(NanoboardCategory, Int)]] = {
    db.run(Category.list())
  }

  override def get(thread: String, offset: Int, count: Int): Future[Seq[(NanoboardMessage, Int)]] = {
    db.run(Post.thread(thread, offset, count))
  }

  override def delete(message: String): Future[Unit] = {
    db.run(Post.delete(message))
  }

  override def reply(parent: String, text: String): Future[NanoboardMessage] = {
    val newMessage: NanoboardMessage = NanoboardMessage.newMessage(parent, text)
    db.run(Post.addReply(newMessage)).map(_ ⇒ newMessage)
  }

  override def updatePlaces(places: Seq[String]): Future[Unit] = {
    db.run(Place.update(places))
  }

  override def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit] = {
    db.run(Category.update(categories))
  }
}
