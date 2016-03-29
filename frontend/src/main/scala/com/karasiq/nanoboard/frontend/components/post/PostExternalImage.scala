package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.Icons
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._

private[components] object PostExternalImage {
  def apply(url: String)(implicit ctx: Ctx.Owner): PostExternalImage = {
    new PostExternalImage(url)
  }
}

private[components] final class PostExternalImage(url: String)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Span] {
  val opened = Var(false)
  val expanded = Var(false)

  private val imageStyleMod = Rx[AutoModifier] {
    val modifier: Modifier = if (expanded()) {
      Seq[Modifier](maxWidth := 100.pct, maxHeight := 100.pct)
    } else {
      Seq[Modifier](maxWidth := 200.px, maxHeight := 200.px)
    }
    modifier
  }

  private val image = Rx[Frag] {
    if (opened()) {
      img(display.block, src := url, imageStyleMod, onclick := Bootstrap.jsClick(_ ⇒ expanded() = !expanded.now))
    } else {
      ""
    }
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(href := url, fontWeight.bold, Icons.image, url, onclick := Bootstrap.jsClick(_ ⇒ opened() = !opened.now)),
      image,
      md
    )
  }
}
