package com.karasiq.nanoboard.frontend

import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.XMLHttpRequest
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

case class NanoboardCategory(hash: String, name: String)
case class NanoboardReply(parent: String, message: String)
case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

object NanoboardApi {
  private def readResponse[T: Reader](response: XMLHttpRequest): T = {
    if (response.status == 200) {
      read[T](response.responseText)
    } else {
      throw new IllegalArgumentException(s"Server error: ${response.status} ${response.statusText} (${response.responseText})")
    }
  }

  def categories()(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get("/categories")
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def thread(hash: String, offset: Int, count: Int)(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts/$hash?offset=$offset&count=$count")
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def addReply(hash: String, message: String)(implicit ec: ExecutionContext): Future[NanoboardMessageData] = {
    Ajax.post(s"/post", write(NanoboardReply(hash, message)))
      .map(readResponse[NanoboardMessageData])
  }

  def delete(hash: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.delete(s"/post/$hash").map(_ ⇒ ())
  }

  def places()(implicit ec: ExecutionContext): Future[Seq[String]] = {
    Ajax.get("/places")
      .map(readResponse[Seq[String]])
  }

  def setPlaces(newList: Seq[String])(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.put("/places", write(newList))
      .map(_ ⇒ ())
  }

  def setCategories(newList: Seq[NanoboardCategory])(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.put("/categories", write(newList))
      .map(_ ⇒ ())
  }
}
