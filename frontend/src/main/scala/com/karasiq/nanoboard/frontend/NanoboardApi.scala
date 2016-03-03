package com.karasiq.nanoboard.frontend

import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

object NanoboardApi {
  def categories()(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts")
      .map(r ⇒ read[Vector[NanoboardMessageData]](r.responseText))
  }

  def answers(hash: String)(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts/$hash")
      .map(r ⇒ read[Vector[NanoboardMessageData]](r.responseText))
  }

  // TODO: Posting
}
