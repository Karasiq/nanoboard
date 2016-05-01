package com.karasiq.nanoboard.frontend.components.post.actions

import com.karasiq.bootstrap.Bootstrap
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{Button, ButtonStyle}
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.modal.Modal
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.post.PostInlineImage
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Blobs, CancelledException, Images, Notifications}
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.File
import rx._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import scalatags.JsDom.all._

case class ImageData(base64: String, format: String)

private[components] object ImageAttachDialog {
  def apply()(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): ImageAttachDialog = {
    new ImageAttachDialog()
  }
}

private[components] final class ImageAttachDialog(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) {
  import controller.locale
  val scale = Var("50")
  val size = Var("500")
  val quality = Var("50")
  val sharpness = Var("50")
  val file = Var[Option[File]](None)
  val useServer = Var(false)

  val formatSelect = FormInput.select(locale.imageFormat, Rx {
    if (!useServer() && Images.isWebpSupported) {
      Seq("jpeg", "webp", "png")
    } else {
      Seq("jpeg", "png")
    }
  })

  def format = formatSelect.selected.map(_.head)

  val ready = Rx {
    def isValidPct(value: Rx[String]): Boolean = Try(value().toInt).filter((1 to 100).contains).isSuccess
    file().nonEmpty && ((useServer() && Try(size().toInt).filter(_ > 0).isSuccess) || isValidPct(scale)) &&
      isValidPct(quality) && (useServer() || isValidPct(sharpness))
  }

  def generate(): Future[ImageData] = {
    val promise = Promise[ImageData]
    val preview = Var[Option[PostInlineImage]](None)
    val modal = Modal(locale.insertImage)
      .withBody(Form(
        formatSelect,
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
            case Success(ImageData(base64, format)) ⇒
              preview() = Some(PostInlineImage(base64, format))

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

  private def createBase64Image(): Future[ImageData] = {
    if (file.now.exists(_.`type` == "image/svg+xml")) {
      Blobs.asBase64(file.now.get).map(ImageData(_, "svg+xml"))
    } else if (useServer.now) {
      NanoboardApi.generateAttachment(format.now, size.now.toInt, quality.now.toInt, file.now.get).map(ImageData(_, format.now))
    } else {
      Images.compress(file.now.get, s"image/${format.now}", scale.now.toInt, quality.now.toInt, sharpness.now.toInt).map(ImageData(_, format.now))
    }
  }
}
