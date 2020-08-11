package com.karasiq.nanoboard.frontend.components.post

import rx._
import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.utils.Blobs

private[components] object PostInlineImage {
  def defaultType = "jpeg"

  def apply(base64: String, imageType: String = defaultType)(implicit controller: NanoboardController): PostInlineImage = {
    new PostInlineImage(base64, imageType)
  }
}

private[components] final class PostInlineImage(val base64: String, val imageType: String)(implicit controller: NanoboardController)
  extends BootstrapHtmlComponent {

  val blobUrl = Blobs.asUrl(Blobs.fromBase64(base64, s"image/$imageType")) // s"data:image/jpeg;base64,$base64"
  val expanded = Var(false)

  private val styleMod = Rx {
    val modifier: Modifier = if (expanded()) {
      Seq[Modifier](maxWidth := 100.pct, maxHeight := 100.pct)
    } else {
      Seq[Modifier](maxWidth := 200.px, maxHeight := 200.px)
    }
    modifier
  }

  override def renderTag(md: Modifier*) = {
    img(alt := controller.locale.embeddedImage, src := blobUrl, styleMod.auto, onclick := Callback.onClick(_ ⇒ expanded() = !expanded.now), md)
  }
}
