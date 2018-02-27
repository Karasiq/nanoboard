package com.karasiq.nanoboard.frontend.components.post

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import rx._
import rx.async._
import scalaTags.all._
import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Mouse

private[components] object PostLink {
  def apply(hash: String)(implicit controller: NanoboardController): PostLink = {
    new PostLink(hash)
  }
}

private[components] final class PostLink(hash: String)(implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  lazy val post = NanoboardApi.post(hash).toRx(None)
    .map(_.map(data ⇒ div(Mouse.relative(xOffset = 12), zIndex := 1, NanoboardPost(showParent = false, showAnswers = true, data)).render))

  private val hover = Var(false)

  override def renderTag(md: Modifier*): TagT = {
    val updateHover: Modifier = Seq(
      onmouseover := { () ⇒
        hover() = true
      }, onmouseout := { () ⇒
        hover() = false
      }
    )

    span(
      position.relative,
      a(updateHover, href := s"#$hash", onclick := Callback.onClick(_ ⇒ controller.showPost(hash)), md),
      Rx[Frag](if (hover() && post().nonEmpty) post().get else "")
    )
  }
}
