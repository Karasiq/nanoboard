package com.karasiq.nanoboard.frontend.components.post.actions

import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

import org.scalajs.dom.raw.File
import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._
import scalatags.JsDom.all._

import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.post.PostInlineImage
import com.karasiq.nanoboard.frontend.utils.{Blobs, CancelledException, Images, Notifications}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout

case class ImageData(base64: String, format: String)

private[components] object ImageAttachDialog {
  def apply()(implicit controller: NanoboardController): ImageAttachDialog = {
    new ImageAttachDialog()
  }
}

private[components] final class ImageAttachDialog(implicit controller: NanoboardController) {
  import controller.locale

  val scale = Var("50")
  val size = Var("500")
  val quality = Var("50")
  val sharpness = Var("50")
  val files = Var[Seq[File]](Nil)
  val useServer = Var(false)

  val formatSelect = FormInput.select(locale.imageFormat, Rx {
    val options = if (!useServer() && Images.isWebpSupported) {
      Seq("jpeg", "webp", "png")
    } else {
      Seq("jpeg", "png")
    }
    options.map(str ⇒ FormSelectOption(str, str))
  })

  lazy val format = formatSelect.selected.map(_.head)

  lazy val ready = Rx {
    def isValidPct(value: Rx[String]): Boolean = Try(value().toInt).filter((1 to 100).contains).isSuccess
    files().nonEmpty && ((useServer() && Try(size().toInt).filter(_ > 0).isSuccess) || isValidPct(scale)) &&
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
        FormInput.file(locale.dataContainer, name := "image", files.reactiveInputRead),
        FormInput.checkbox(locale.useServerRendering, useServer.reactiveInput),
        div(preview.map(_.map(_.base64.length).fold[Frag]("")(length ⇒ s"$length ${locale.bytes}"))),
        div(preview.map(_.fold[Frag]("")(img ⇒ img))),
        onsubmit := Callback.onSubmit(_ ⇒ ())
      ))
      .withButtons(
        Modal.closeButton(locale.cancel)(onclick := Callback.onClick { _ ⇒
          promise.failure(CancelledException)
        }),
        Button(ButtonStyle.info)(locale.preview, ready.reactiveShow)(onclick := Callback.onClick { _ ⇒
          createBase64Image().onComplete {
            case Success(ImageData(base64, format)) ⇒
              preview() = Some(PostInlineImage(base64, format))

            case Failure(exc) ⇒
              Notifications.error(exc)(locale.attachmentGenerationError, Layout.topRight)
              preview() = None
          }
        }),
        Button(ButtonStyle.success)(locale.submit, Modal.dismiss, ready.reactiveShow, onclick := Callback.onClick { _ ⇒
          promise.completeWith(createBase64Image())
        })
      )
    modal.show(backdrop = false)
    promise.future
  }

  private def createBase64Image(): Future[ImageData] = files.now.headOption match {
    case Some(file) if file.`type` ==  "image/svg+xml" ⇒
      Blobs.asBase64(file).map(ImageData(_, "svg+xml"))

    case Some(file) if useServer.now ⇒
      NanoboardApi.generateAttachment(format.now, size.now.toInt, quality.now.toInt, file).map(ImageData(_, format.now))

    case Some(file) ⇒
      Images.compress(file, s"image/${format.now}", scale.now.toInt, quality.now.toInt, sharpness.now.toInt).map(ImageData(_, format.now))

    case None ⇒
      Future.failed(new NoSuchElementException("No file selected"))
  }
}
