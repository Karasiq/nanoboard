package com.karasiq.nanoboard.frontend.components.post

import scala.scalajs.js.URIUtils

import rx._
import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.Icons

private[components] object PostFractalMusic {
  def apply(formula: String): PostFractalMusic = {
    new PostFractalMusic(formula)
  }
}

private[components] final class PostFractalMusic(formula: String) extends BootstrapHtmlComponent {
  val opened = Var(false)
  val url = s"/fractal_music/${URIUtils.encodeURIComponent(formula)}"

  private val player = Rx[Frag] {
    if (opened()) {
      audio(attr("controls") := "controls", attr("autoplay") := true, attr("loop") := true, display.block, `type` := "audio/wav", src := url)
    } else {
      ""
    }
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(href := url, fontWeight.bold, Icons.music, formula, onclick := Callback.onClick(_ ⇒ opened() = !opened.now)),
      player,
      md
    )
  }
}