package com.karasiq.nanoboard.frontend

import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.XMLHttpRequest
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

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
    Ajax.get(s"/posts")
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
}
