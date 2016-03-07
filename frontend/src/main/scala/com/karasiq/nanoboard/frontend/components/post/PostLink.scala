package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.NanoboardContext
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.NanoboardController
import org.scalajs.dom
import rx._
import rx.async._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{span => _}
import scala.language.postfixOps
import scalatags.JsDom.all._

private[components] object PostLink {
  def apply(hash: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): PostLink = {
    new PostLink(hash)
  }
}

private[components] final class PostLink(hash: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Span] {
  lazy val post = NanoboardApi.post(hash).toRx(None).map(_.map(NanoboardPost(false, true, _).renderTag().render))

  private val hover = Var(false)

  override def renderTag(md: Modifier*): RenderedTag = {
    val updateHover: Modifier = Seq(
      onmouseover := { () ⇒
        hover() = true
      }, onmouseout := { () ⇒
        hover() = false
      }
    )

    span(
      a(updateHover, href := s"#$hash", onclick := Bootstrap.jsClick { _ ⇒
        controller.setContext(NanoboardContext.Thread(hash, 0))
      }, md),
      Rx[Frag] {
        if (hover() && post().nonEmpty) {
          div(position.absolute, top := 60.px, left := 170.px, zIndex := 1, post())
        } else {
          ""
        }
      }
    )
  }
}
