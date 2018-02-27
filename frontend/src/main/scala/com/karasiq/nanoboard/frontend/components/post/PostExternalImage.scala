package com.karasiq.nanoboard.frontend.components.post

import scala.language.postfixOps

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.frontend.Icons

private[components] object PostExternalImage {
  def apply(url: String): PostExternalImage = {
    new PostExternalImage(url)
  }
}

private[components] final class PostExternalImage(url: String) extends BootstrapHtmlComponent {
  val opened = Var(false)
  val expanded = Var(false)

  private val imageStyleMod = Rx {
    val modifier: Modifier = if (expanded()) {
      Seq[Modifier](maxWidth := 100.pct, maxHeight := 100.pct)
    } else {
      Seq[Modifier](maxWidth := 200.px, maxHeight := 200.px)
    }
    modifier
  }

  private val image = Rx[Frag] {
    if (opened()) {
      img(display.block, src := url, imageStyleMod.auto, onclick := Callback.onClick(_ ⇒ expanded() = !expanded.now))
    } else {
      ""
    }
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(href := url, fontWeight.bold, Icons.image, url, onclick := Callback.onClick(_ ⇒ opened() = !opened.now)),
      image,
      md
    )
  }
}
