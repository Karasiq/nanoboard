package com.karasiq.nanoboard.frontend.api


import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import boopickle.Default._
import org.scalajs.dom.{console, Blob}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.XMLHttpRequest

import com.karasiq.nanoboard.api._

/**
  * Nanoboard REST API
  */
object NanoboardApi {
  private val marshaller = BinaryMarshaller
  
  private def readResponse[T: Pickler](response: XMLHttpRequest): T = {
    if (response.status == 200) {
      marshaller.read[T](response.response)
    } else {
      val message = s"Nanoboard API error: ${response.status} ${response.statusText}"
      console.error(message)
      throw new IllegalArgumentException(message)
    }
  }

  def categories(): Future[Vector[NanoboardMessageData]] = {
    Ajax.get("/categories", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def post(hash: String): Future[Option[NanoboardMessageData]] = {
    Ajax.get(s"/post/$hash", responseType = marshaller.responseType)
      .map(readResponse[Option[NanoboardMessageData]])
  }

  def thread(hash: String, offset: Long, count: Long): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts/$hash?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def addReply(hash: String, message: String): Future[NanoboardMessageData] = {
    Ajax.post(s"/post", marshaller.write(NanoboardReply(hash, message)), responseType = marshaller.responseType)
      .map(readResponse[NanoboardMessageData])
  }

  def markAsPending(hash: String): Future[Unit] = {
    Ajax.put(s"/pending/$hash").map(_ ⇒ ())
  }

  def markAsNotPending(hash: String): Future[Unit] = {
    Ajax.delete(s"/pending/$hash").map(_ ⇒ ())
  }

  def delete(hash: String): Future[Seq[String]] = {
    Ajax.delete(s"/post/$hash", responseType = marshaller.responseType)
      .map(readResponse[Seq[String]])
  }

  def clearDeleted(): Future[Int] = {
    Ajax.delete("/deleted", responseType = marshaller.responseType)
      .map(readResponse[Int])
  }

  def delete(offset: Int, count: Int): Future[Seq[String]] = {
    Ajax.delete(s"/posts?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Seq[String]])
  }

  def places(): Future[Seq[String]] = {
    Ajax.get("/places", responseType = marshaller.responseType)
      .map(readResponse[Seq[String]])
  }

  def setPlaces(newList: Seq[String]): Future[Unit] = {
    Ajax.put("/places", marshaller.write(newList))
      .map(_ ⇒ ())
  }

  def setCategories(newList: Seq[NanoboardCategory]): Future[Unit] = {
    Ajax.put("/categories", marshaller.write(newList))
      .map(_ ⇒ ())
  }

  def pending(offset: Long, count: Long): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/pending?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def recent(offset: Long, count: Long): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def generateContainer(pending: Int, random: Int, format: String, container: Ajax.InputData): Future[Blob] = {
    Ajax.post(s"/container?pending=$pending&random=$random&format=$format", container, responseType = "blob")
      .map { r ⇒
        if (r.status == 200) {
          r.response.asInstanceOf[Blob]
        } else {
          throw new IllegalArgumentException(s"${r.status} ${r.statusText}")
        }
      }
  }

  def generateAttachment(format: String, size: Int, quality: Int, container: Ajax.InputData): Future[String] = {
    Ajax.post(s"/attachment?format=$format&size=$size&quality=$quality", container)
      .map(_.responseText)
  }

  def containers(offset: Long, count: Long): Future[Vector[NanoboardContainer]] = {
    Ajax.get(s"/containers?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardContainer]])
  }

  def clearContainer(id: Long): Future[Vector[String]] = {
    Ajax.delete(s"/posts?container=$id", responseType = marshaller.responseType)
      .map(readResponse[Vector[String]])
  }

  def requestVerification(hash: String): Future[NanoboardCaptchaRequest] = {
    Ajax.get(s"/verify/$hash", responseType = marshaller.responseType)
      .map(readResponse[NanoboardCaptchaRequest])
  }

  def verifyPost(request: NanoboardCaptchaRequest, answer: String): Future[NanoboardMessageData] = {
    Ajax.post(s"/verify", marshaller.write(NanoboardCaptchaAnswer(request, answer)), responseType = marshaller.responseType)
      .map(readResponse[NanoboardMessageData])
  }
}
