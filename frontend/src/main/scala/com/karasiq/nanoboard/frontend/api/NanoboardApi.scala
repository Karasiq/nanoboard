package com.karasiq.nanoboard.frontend.api


import boopickle.Default._
import com.karasiq.nanoboard.api._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.{File, XMLHttpRequest}
import org.scalajs.dom.{Blob, console}

import scala.concurrent.{ExecutionContext, Future}

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

  def categories()(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get("/categories", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def post(hash: String)(implicit ec: ExecutionContext): Future[Option[NanoboardMessageData]] = {
    Ajax.get(s"/post/$hash", responseType = marshaller.responseType)
      .map(readResponse[Option[NanoboardMessageData]])
  }

  def thread(hash: String, offset: Long, count: Long)(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts/$hash?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def addReply(hash: String, message: String)(implicit ec: ExecutionContext): Future[NanoboardMessageData] = {
    Ajax.post(s"/post", marshaller.write(NanoboardReply(hash, message)), responseType = marshaller.responseType)
      .map(readResponse[NanoboardMessageData])
  }

  def markAsPending(hash: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.put(s"/pending/$hash").map(_ ⇒ ())
  }

  def markAsNotPending(hash: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.delete(s"/pending/$hash").map(_ ⇒ ())
  }

  def delete(hash: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    Ajax.delete(s"/post/$hash", responseType = marshaller.responseType)
      .map(readResponse[Seq[String]])
  }

  def clearDeleted()(implicit ec: ExecutionContext): Future[Int] = {
    Ajax.delete("/deleted", responseType = marshaller.responseType)
      .map(readResponse[Int])
  }

  def delete(offset: Int, count: Int)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    Ajax.delete(s"/posts?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Seq[String]])
  }

  def places()(implicit ec: ExecutionContext): Future[Seq[String]] = {
    Ajax.get("/places", responseType = marshaller.responseType)
      .map(readResponse[Seq[String]])
  }

  def setPlaces(newList: Seq[String])(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.put("/places", marshaller.write(newList))
      .map(_ ⇒ ())
  }

  def setCategories(newList: Seq[NanoboardCategory])(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.put("/categories", marshaller.write(newList))
      .map(_ ⇒ ())
  }

  def pending(offset: Long, count: Long)(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/pending?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def recent(offset: Long, count: Long)(implicit ec: ExecutionContext): Future[Vector[NanoboardMessageData]] = {
    Ajax.get(s"/posts?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardMessageData]])
  }

  def generateContainer(pending: Int, random: Int, format: String, container: File)(implicit ec: ExecutionContext): Future[Blob] = {
    Ajax.post(s"/container?pending=$pending&random=$random&format=$format", container, responseType = "blob")
      .map { r ⇒
        if (r.status == 200) {
          r.response.asInstanceOf[Blob]
        } else {
          throw new IllegalArgumentException(s"${r.status} ${r.statusText}")
        }
      }
  }

  def generateAttachment(format: String, size: Int, quality: Int, container: File)(implicit ec: ExecutionContext): Future[String] = {
    Ajax.post(s"/attachment?format=$format&size=$size&quality=$quality", container)
      .map(_.responseText)
  }

  def containers(offset: Long, count: Long)(implicit ec: ExecutionContext): Future[Vector[NanoboardContainer]] = {
    Ajax.get(s"/containers?offset=$offset&count=$count", responseType = marshaller.responseType)
      .map(readResponse[Vector[NanoboardContainer]])
  }

  def clearContainer(id: Long)(implicit ec: ExecutionContext): Future[Vector[String]] = {
    Ajax.delete(s"/posts?container=$id", responseType = marshaller.responseType)
      .map(readResponse[Vector[String]])
  }

  def requestVerification(hash: String)(implicit ec: ExecutionContext): Future[NanoboardCaptchaRequest] = {
    Ajax.get(s"/verify/$hash", responseType = marshaller.responseType)
      .map(readResponse[NanoboardCaptchaRequest])
  }

  def verifyPost(request: NanoboardCaptchaRequest, answer: String)(implicit ec: ExecutionContext): Future[NanoboardMessageData] = {
    Ajax.post(s"/verify", marshaller.write(NanoboardCaptchaAnswer(request, answer)), responseType = marshaller.responseType)
      .map(readResponse[NanoboardMessageData])
  }
}
