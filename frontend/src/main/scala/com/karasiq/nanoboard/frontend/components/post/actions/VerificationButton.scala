package com.karasiq.nanoboard.frontend.components.post.actions

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._
import scalatags.JsDom.all._

import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.nanoboard.frontend.utils.{CancelledException, Notifications}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout

private[post] object VerificationButton {
  def apply(post: NanoboardMessageData)(implicit controller: NanoboardController) = {
    new VerificationButton(post)
  }
}

private[post] final class VerificationButton(post: NanoboardMessageData)(implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  import controller.{locale, style}
  val hidden = Var(post.isSigned || post.isCategory)

  override def renderTag(md: Modifier*): TagT = {
    a(style.postLink, href := "#", Icons.verify, locale.verify, Rx(if (hidden()) display.none else display.inline).auto, onclick := Callback.onClick { _ ⇒
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
