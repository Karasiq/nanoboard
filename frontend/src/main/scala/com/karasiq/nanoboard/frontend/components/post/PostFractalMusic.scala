package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.Icons
import org.scalajs.dom
import rx._

import scala.scalajs.js.URIUtils
import scalatags.JsDom.all._

private[components] object PostFractalMusic {
  def apply(formula: String)(implicit ctx: Ctx.Owner): PostFractalMusic = {
    new PostFractalMusic(formula)
  }
}

private[components] final class PostFractalMusic(formula: String)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Span] {
  val opened = Var(false)

  val url = s"/fractal_music/${URIUtils.encodeURIComponent(formula)}"

  private val player = Rx[Frag] {
    if (opened()) {
      audio("controls".attr := "controls", "autoplay".attr := true, "loop".attr := true, display.block, `type` := "audio/wav", src := url)
    } else {
      ""
    }
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(href := url, fontWeight.bold, Icons.music, formula, onclick := Bootstrap.jsClick(_ ⇒ opened() = !opened.now)),
      player,
      md
    )
  }
}