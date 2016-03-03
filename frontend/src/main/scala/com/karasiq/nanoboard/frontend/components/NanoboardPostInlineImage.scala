package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._

final class NanoboardPostInlineImage(base64: String)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Image] {
  val state = Var(false)

  override def renderTag(md: Modifier*) = {
    val style = Rx[AutoModifier] {
      val modifier: Modifier = if (state()) {
        Seq[Modifier](maxWidth := 100.pct, maxHeight := 100.pct)
      } else {
        Seq[Modifier](maxWidth := 200.px, maxHeight := 200.px)
      }
      modifier
    }

    img(alt := "Embedded image", src := s"data:image/png;base64,$base64", style, onclick := Bootstrap.jsClick { _ ⇒
      state() = !state.now
    })
  }
}
