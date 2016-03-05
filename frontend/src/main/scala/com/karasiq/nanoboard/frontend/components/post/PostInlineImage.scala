package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.utils.Blobs
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._

private[components] object PostInlineImage {
  def apply(base64: String)(implicit ctx: Ctx.Owner): PostInlineImage = {
    new PostInlineImage(base64)
  }
}

private[components] final class PostInlineImage(base64: String)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Image] {
  val state = Var(false)

  private val styleMod = Rx[AutoModifier] {
    val modifier: Modifier = if (state()) {
      Seq[Modifier](maxWidth := 100.pct, maxHeight := 100.pct)
    } else {
      Seq[Modifier](maxWidth := 200.px, maxHeight := 200.px)
    }
    modifier
  }

  override def renderTag(md: Modifier*) = {
    val blobUrl = Blobs.asUrl(Blobs.asBlob(base64, "image/jpeg")) // s"data:image/jpeg;base64,$base64"
    img(alt := "Embedded image", src := blobUrl, styleMod, onclick := Bootstrap.jsClick { _ ⇒
      state() = !state.now
    }, md)
  }
}
