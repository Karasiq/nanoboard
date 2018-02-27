package com.karasiq.nanoboard.frontend.components.post.actions

import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.api.{NanoboardCaptchaRequest, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.{Blobs, CancelledException, Notifications}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout

private[components] object CaptchaDialog {
  def apply()(implicit controller: NanoboardController): CaptchaDialog = {
    new CaptchaDialog()
  }
}

/**
  * Captcha dialog
  */
private[components] final class CaptchaDialog(implicit controller: NanoboardController) {
  import controller.locale

  val answer = Var("")

  val ready = Rx {
    answer().nonEmpty
  }

  def verify(hash: String): Future[NanoboardMessageData] = {
    for {
      request ← NanoboardApi.requestVerification(hash)
      result ← solveCaptcha(request)
    } yield result
  }

  def solveCaptcha(request: NanoboardCaptchaRequest): Future[NanoboardMessageData] = {
    val promise = Promise[NanoboardMessageData]
    val modal = Modal(locale.verify)
      .withBody(Form(
        img(display.block, height := 60.px, src := Blobs.asUrl(Blobs.fromBytes(request.captcha.image, "image/png"))),
        FormInput.text((), answer.reactiveInput),
        onsubmit := Callback.onSubmit(_ ⇒ ())
      ))
      .withButtons(
        Modal.closeButton(locale.cancel)(onclick := Callback.onClick { _ ⇒
          promise.failure(CancelledException)
        }),
        Button(ButtonStyle.success)(locale.submit, Modal.dismiss, ready.reactiveShow, onclick := Callback.onClick { _ ⇒
          NanoboardApi.verifyPost(request, answer.now) onComplete {
            case Success(data) ⇒
              promise.success(data)

            case Failure(exc) ⇒
              Notifications.error(exc)(locale.verificationError, Layout.topRight)
              promise.completeWith(solveCaptcha(request)) // Retry
          }
        })
      )
    modal.show(backdrop = false)
    promise.future
  }
}
