package com.karasiq.nanoboard.frontend.utils

import com.karasiq.bootstrap.BootstrapImplicits._
import org.scalajs.dom
import org.scalajs.dom.raw._
import org.scalajs.dom.{Blob, Event}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.typedarray.Uint8Array
import scalatags.JsDom.all._

// Blob/file util
object Blobs {
  private def urlObject: URL = {
    js.Dynamic.global.window.URL.asInstanceOf[UndefOr[URL]]
      .orElse(js.Dynamic.global.window.webkitURL.asInstanceOf[UndefOr[URL]])
      .get
  }

  def asBlob(base64: String, contentType: String = ""): Blob = {
    import scala.scalajs.js.JSConverters._
    val array = new Uint8Array(dom.window.atob(base64).toCharArray.map(_.toByte).toJSArray)
    new Blob(js.Array(array), BlobPropertyBag(contentType))
  }

  def saveBlob(blob: Blob, fileName: String): Unit = {
    val url = urlObject.createObjectURL(blob)
    val anchor = a(href := url, "download".attr := fileName, target := "_blank", display.none).render
    dom.document.body.appendChild(anchor)
    dom.window.setTimeout(() ⇒ {
      dom.document.body.removeChild(anchor)
      urlObject.revokeObjectURL(url)
    }, 500)
    anchor.click()
  }

  def asUrl(blob: Blob): String = {
    urlObject.createObjectURL(blob)
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
