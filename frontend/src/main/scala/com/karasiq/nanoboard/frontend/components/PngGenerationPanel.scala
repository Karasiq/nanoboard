package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.model.ThreadModel
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Blobs, Notifications}
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import org.scalajs.dom
import org.scalajs.dom.Blob
import org.scalajs.dom.html.Input
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

object PngGenerationPanel {
  def apply()(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): PngGenerationPanel = {
    new PngGenerationPanel
  }
}

final class PngGenerationPanel(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
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
    FormInput.text(locale.imageFormat, style.input, name := "format", value := "png"),
    FormInput.file(locale.dataContainer, style.input, name := "container"),
    Form.submit(locale.generateContainer)(style.submit, "disabled".classIf(loading), "btn-block".addClass),
    onsubmit := Bootstrap.jsSubmit { frm ⇒
      if (!loading.now) {
        loading() = true
        def input(name: String) = frm(name).asInstanceOf[Input]
        val file: Blob = input("container").files.headOption.getOrElse(Blobs.fromBytes(Array.emptyByteArray))
        val pending = input("pending").valueAsNumber
        val random = input("random").valueAsNumber
        val format: String = input("format").value

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
