package com.karasiq.nanoboard.frontend.utils

import org.scalajs.dom.Blob

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

@js.native
private[utils] trait ImageUtil extends js.Object {
  def sharpen(ctx: js.Dynamic, width: Int, height: Int, sharpness: Double): Unit = js.native
  def drawImage(file: Blob, compress: Boolean = true, format: String = "image/jpeg", scale: Double, quality: Double, sharpness: Double, success: js.Function): Unit = js.native
}

object Images {
  @js.native
  @JSName("img2base64")
  private object ImageUtil extends ImageUtil

  def compress(data: Blob, format: String = "image/jpeg", scale: Int = 100, quality: Int = 100, sharpness: Int = 100): Future[String] = {
    assert(sharpness > 0 && scale > 0 && quality > 0 && quality <= 100)
    val promise = Promise[String]
    ImageUtil.drawImage(data, compress = true, format, scale, quality, sharpness, { (url: String) ⇒
      promise.success(url.split(",", 2).last)
    })
    promise.future
  }
}
