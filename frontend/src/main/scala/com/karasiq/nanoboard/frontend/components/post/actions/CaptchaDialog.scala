package com.karasiq.nanoboard.frontend.components.post.actions

import com.karasiq.bootstrap.Bootstrap
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{Button, ButtonStyle}
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.modal.Modal
import com.karasiq.nanoboard.api.{NanoboardCaptchaRequest, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Blobs, CancelledException, Notifications}
import rx._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[components] object CaptchaDialog {
  def apply()(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): CaptchaDialog = {
    new CaptchaDialog()
  }
}

/**
  * Captcha dialog
  */
private[components] final class CaptchaDialog(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) {
  import controller.locale

  val answer = Var("")

  val ready = Rx {
    answer().nonEmpty
  }

  def verify(hash: String): Future[NanoboardMessageData] = {
    val promise = Promise[NanoboardMessageData]
    NanoboardApi.requestVerification(hash).onComplete {
      case Success(request) ⇒
        def solveCaptcha(request: NanoboardCaptchaRequest): Unit = {
          val modal = Modal(locale.verify)
            .withBody(Form(
              img(display.block, height := 60.px, src := Blobs.asUrl(Blobs.fromBytes(request.captcha.image, "image/png"))),
              FormInput.text((), answer.reactiveInput),
              onsubmit := Bootstrap.jsSubmit(_ ⇒ ())
            ))
            .withButtons(
              Modal.closeButton(locale.cancel)(onclick := Bootstrap.jsClick { _ ⇒
                promise.failure(CancelledException)
              }),
              Button(ButtonStyle.success)(locale.submit, Modal.dismiss, ready.reactiveShow, onclick := Bootstrap.jsClick { _ ⇒
                NanoboardApi.verifyPost(request, answer.now) onComplete {
                  case Success(data) ⇒
                    promise.success(data)

                  case Failure(exc) ⇒
                    Notifications.error(exc)(locale.verificationError, Layout.topRight)
                    solveCaptcha(request) // Retry
                }
              })
            )
          modal.show(backdrop = false)
        }
        solveCaptcha(request)

      case Failure(exc) ⇒
        promise.failure(exc)
    }

    promise.future
  }
}
