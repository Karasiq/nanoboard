package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.Bootstrap
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{Button, ButtonStyle}
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.modal.Modal
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{CancelledException, Images, Notifications}
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.File
import rx._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import scalatags.JsDom.all._

private[components] object AttachmentGenerationDialog {
  def apply()(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): AttachmentGenerationDialog = {
    new AttachmentGenerationDialog
  }
}

private[components] final class AttachmentGenerationDialog(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) {
  import controller.locale
  val format = Var("jpeg")
  val scale = Var("50")
  val size = Var("500")
  val quality = Var("50")
  val sharpness = Var("50")
  val file = Var[Option[File]](None)
  val useServer = Var(false)

  val ready = Rx {
    def isValidPct(value: Rx[String]): Boolean = Try(value().toInt).filter((1 to 100).contains).isSuccess
    format().nonEmpty && file().nonEmpty &&
      ((useServer() && Try(size().toInt).filter(_ > 0).isSuccess) || isValidPct(scale)) &&
      isValidPct(quality) && (useServer() || isValidPct(sharpness))
  }

  def generate(): Future[String] = {
    val promise = Promise[String]
    val preview = Var[Option[PostInlineImage]](None)
    val modal = Modal(locale.insertImage)
      .withBody(Form(
        FormInput.text(locale.imageFormat, name := "format", format.reactiveInput, placeholder := "jpeg"),
        Rx {
          if (useServer()) FormInput.number(locale.imageSize, name := "size", min := 1, size.reactiveInput, placeholder := 500)
          else FormInput.number(locale.imageScale, name := "scale", min := 1, max := 100, scale.reactiveInput, placeholder := 50)
        },
        FormInput.number(locale.imageQuality, name := "quality", min := 1, max := 100, quality.reactiveInput, placeholder := 50),
        Rx[Frag] {
          if (useServer()) "" else FormInput.number(locale.imageSharpness, name := "sharpness", min := 1, max := 100, sharpness.reactiveInput, placeholder := 50)
        },
        FormInput.file(locale.dataContainer, name := "image", file.reactiveRead("change", e ⇒ e.asInstanceOf[Input].files.headOption)),
        FormInput.checkbox(locale.useServerRendering, useServer.reactiveInput),
        div(preview.map(_.map(_.base64.length).fold[Frag]("")(length ⇒ s"$length ${locale.bytes}"))),
        div(preview.map(_.fold[Frag]("")(img ⇒ img))),
        onsubmit := Bootstrap.jsSubmit(_ ⇒ ())
      ))
      .withButtons(
        Modal.closeButton(locale.cancel)(onclick := Bootstrap.jsClick { _ ⇒
          promise.failure(CancelledException)
        }),
        Button(ButtonStyle.info)(locale.preview, ready.reactiveShow)(onclick := Bootstrap.jsClick { _ ⇒
          createBase64Image().onComplete {
            case Success(base64) ⇒
              preview() = Some(PostInlineImage(base64, s"image/${format.now}"))

            case Failure(exc) ⇒
              Notifications.error(exc)(locale.attachmentGenerationError, Layout.topRight)
              preview() = None
          }
        }),
        Button(ButtonStyle.success)(locale.submit, Modal.dismiss, ready.reactiveShow, onclick := Bootstrap.jsClick { _ ⇒
          promise.completeWith(createBase64Image())
        })
      )
    modal.show(backdrop = false)
    promise.future
  }

  private def createBase64Image(): Future[String] = {
    if (useServer.now) {
      NanoboardApi.generateAttachment(format.now, size.now.toInt, quality.now.toInt, file.now.get)
    } else {
      Images.compress(file.now.get, s"image/${format.now}", scale.now.toInt, quality.now.toInt, sharpness.now.toInt)
    }
  }
}
