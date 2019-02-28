package com.karasiq.nanoboard.frontend.utils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import org.scalajs.dom
import org.scalajs.dom.{Blob, Event}
import org.scalajs.dom.raw._
import scalatags.JsDom.all._

/**
  * Blob/file utility
  */
object Blobs {

  def fromBytes(data: Array[Byte], contentType: String = ""): Blob = {
    import scala.scalajs.js.JSConverters._
    val array = new Uint8Array(data.toJSArray)
    new Blob(js.Array(array), BlobPropertyBag(contentType))
  }

  def fromChars(data: Array[Char], contentType: String = ""): Blob = {
    fromBytes(data.map(_.toByte), contentType)
  }

  def fromString(data: String, contentType: String = ""): Blob = {
    fromChars(data.toCharArray, contentType)
  }

  def fromBase64(base64: String, contentType: String = ""): Blob = {
    fromString(
        () ⇒ {
               try{
                  return dom.window.atob(base64);    //return base if base64
               }catch{
                  return "NOT_BASE_PLACEHOLDER_TEXT";//and do not stop JavaScript, after throw error.
               }
        }
       , contentType)
  }

  def saveBlob(blob: Blob, fileName: String): Unit = {
    val url = URL.createObjectURL(blob)
    val anchor = a(href := url, attr("download") := fileName, target := "_blank", display.none).render
    dom.document.body.appendChild(anchor)
    dom.window.setTimeout(() ⇒ {
      dom.document.body.removeChild(anchor)
      URL.revokeObjectURL(url)
    }, 500)
    anchor.click()
  }

  def asUrl(blob: Blob): String = {
    URL.createObjectURL(blob)
  }

  def asDataURL(blob: Blob): Future[String] = {
    val promise = Promise[String]
    val reader = new FileReader
    reader.readAsDataURL(blob)
    reader.onloadend = (_: ProgressEvent) ⇒ {
      promise.success(reader.result.asInstanceOf[String])
    }

    reader.onerror = (e: Event) ⇒ {
      promise.failure(new IllegalArgumentException(e.toString))
    }

    promise.future
  }

  def asBase64(blob: Blob)(implicit ec: ExecutionContext): Future[String] = {
    asDataURL(blob).map(_.split(",", 2).last)
  }
}
