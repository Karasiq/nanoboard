package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Mouse
import org.scalajs.dom
import rx._
import rx.async._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scalatags.JsDom.all._

private[components] object PostLink {
  def apply(hash: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): PostLink = {
    new PostLink(hash)
  }
}

private[components] final class PostLink(hash: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Span] {
  lazy val post = NanoboardApi.post(hash).toRx(None)
    .map(_.map(data ⇒ div(Mouse.relative(xOffset = 12), zIndex := 1, NanoboardPost(showParent = false, showAnswers = true, data)).render))

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
      position.relative,
      a(updateHover, href := s"#$hash", onclick := Bootstrap.jsClick(_ ⇒ controller.showPost(hash)), md),
      Rx[Frag](if (hover() && post().nonEmpty) post().get else "")
    )
  }
}
