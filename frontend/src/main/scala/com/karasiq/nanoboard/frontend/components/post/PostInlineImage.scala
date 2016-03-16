package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.utils.Blobs
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._

private[components] object PostInlineImage {
  def apply(base64: String, imageType: String = "image/jpeg")(implicit ctx: Ctx.Owner, controller: NanoboardController): PostInlineImage = {
    new PostInlineImage(base64, imageType)
  }
}

private[components] final class PostInlineImage(val base64: String, val imageType: String)(implicit ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Image] {
  val expanded = Var(false)

  private val styleMod = Rx[AutoModifier] {
    val modifier: Modifier = if (expanded()) {
      Seq[Modifier](maxWidth := 100.pct, maxHeight := 100.pct)
    } else {
      Seq[Modifier](maxWidth := 200.px, maxHeight := 200.px)
    }
    modifier
  }

  override def renderTag(md: Modifier*) = {
    val blobUrl = Blobs.asUrl(Blobs.asBlob(base64, imageType)) // s"data:image/jpeg;base64,$base64"
    img(alt := controller.locale.embeddedImage, src := blobUrl, styleMod, onclick := Bootstrap.jsClick { _ ⇒
      expanded() = !expanded.now
    }, md)
  }
}
