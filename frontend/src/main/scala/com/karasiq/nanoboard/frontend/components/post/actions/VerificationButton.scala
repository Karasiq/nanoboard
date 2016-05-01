package com.karasiq.nanoboard.frontend.components.post.actions

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{CancelledException, Notifications}
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[post] object VerificationButton {
  def apply(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) = {
    new VerificationButton(post)
  }
}

private[post] final class VerificationButton(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Anchor] {
  import controller.{locale, style}
  val hidden = Var(post.isSigned || post.isCategory)

  override def renderTag(md: Modifier*): RenderedTag = {
    a(style.postLink, href := "#", Icons.verify, locale.verify, Rx[AutoModifier](if (hidden()) display.none else display.inline), onclick := Bootstrap.jsClick { _ ⇒
      hidden() = true
      CaptchaDialog().verify(post.hash).onComplete {
        case Success(verified) ⇒
          Notifications.success(locale.verificationSuccess(verified.hash), Layout.topRight)
          controller.addPending(verified)

        case Failure(CancelledException) ⇒
          hidden() = false

        case Failure(exc) ⇒
          hidden() = false
          Notifications.error(exc)(locale.verificationError, Layout.topRight)
      }
    })
  }
}
