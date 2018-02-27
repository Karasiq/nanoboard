package com.karasiq.nanoboard.frontend.components

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}

import org.scalajs.dom.Blob
import org.scalajs.dom.html.Input
import rx._
import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.model.ThreadModel
import com.karasiq.nanoboard.frontend.utils.{Blobs, Notifications}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout

object PngGenerationPanel {
  def apply()(implicit ec: ExecutionContext, controller: NanoboardController): PngGenerationPanel = {
    new PngGenerationPanel
  }
}

final class PngGenerationPanel(implicit ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent {
  import controller.{locale, style}

  val model = ThreadModel(Var(NanoboardContext.Pending()), 100)

  private val loading = Var(false)

  private val pendingContainer = Rx[Frag] {
    val posts = model.posts()
    if (posts.nonEmpty) div(
      marginTop := 20.px,
      h3(locale.pendingPosts),
      for (p ← posts) yield GridSystem.mkRow(NanoboardPost(showParent = true, showAnswers = false, p))
    ) else ()
  }

  private val form = Form(
    FormInput.number(locale.pendingPosts, style.input, name := "pending", value := 3, min := 0),
    FormInput.number(locale.randomPosts, style.input, name := "random", value := 30, min := 0),
    // FormInput.text(locale.imageFormat, style.input, name := "format", value := "png"),
    FormInput.file(locale.dataContainer, style.input, name := "container"),
    Form.submit(locale.generateContainer)(style.submit, "disabled".classIf(loading), "btn-block".addClass),
    onsubmit := Callback.onSubmit { frm ⇒
      if (!loading.now) {
        loading() = true
        def input(name: String) = frm(name).asInstanceOf[Input]
        val file: Blob = input("container").files.headOption.getOrElse(Blobs.fromBytes(Array.emptyByteArray))
        val pending = input("pending").valueAsNumber
        val random = input("random").valueAsNumber
        val format = "png" // input("format").value

        NanoboardApi.generateContainer(pending, random, format, file).onComplete {
          case Success(blob) ⇒
            loading() = false
            Blobs.saveBlob(blob, s"${js.Date.now()}.$format")
            model.updatePosts()

          case Failure(exc) ⇒
            loading() = false
            Notifications.error(exc)(locale.containerGenerationError, Layout.topRight, 1500)
        }
      }
    }
  )

  override def renderTag(md: Modifier*) = {
    div(form, pendingContainer, marginBottom := 50.px)
  }
}
