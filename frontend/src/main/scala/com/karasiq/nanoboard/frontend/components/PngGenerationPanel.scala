package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{FileSaver, Notifications}
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardContext, NanoboardMessageData}
import org.scalajs.dom
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

final class PngGenerationPanel(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] with NanoboardPostContainer {
  override val posts = Var(Vector.empty[NanoboardMessageData])

  override val context: Var[NanoboardContext] = Var(NanoboardContext.Root)

  private val loading = Var(false)

  override def update(): Unit = {
    loading() = true
    NanoboardApi.pending().onComplete {
      case Success(posts) ⇒
        this.posts() = posts
        loading() = false

      case Failure(_) ⇒
        loading() = false
    }
  }

  private val pendingContainer = Rx[Frag] {
    val posts = this.posts()
    if (posts.nonEmpty) Bootstrap.well(
      marginTop := 20.px,
      h3("Pending posts"),
      for (p ← posts) yield NanoboardPost(isOp = true, p)
    ) else ()
  }

  private val form = Form(
    FormInput.number("Pending posts", name := "pending", value := 10),
    FormInput.number("Random posts", name := "random", value := 50),
    FormInput.text("Output format", name := "format", value := "png"),
    FormInput.file("Data container", name := "container"),
    Form.submit("Generate container image")("disabled".classIf(loading)),
    onsubmit := Bootstrap.jsSubmit { frm ⇒
      if (!loading.now) {
        def input(name: String) = frm(name).asInstanceOf[Input]
        input("container").files.headOption match {
          case Some(file) ⇒
            val pending = input("pending").valueAsNumber
            val random = input("random").valueAsNumber
            val format: String = input("format").value

            NanoboardApi.generateContainer(pending, random, format, file).onComplete {
              case Success(blob) ⇒
                FileSaver.saveBlob(blob, s"${js.Date.now()}.$format")
                update()

              case Failure(exc) ⇒
                Notifications.error(s"Container generation failure: $exc", Layout.topRight, 1500)
            }

          case None ⇒
            Notifications.warning("Container file not selected", Layout.topRight)
        }
      }
    }
  )

  override def renderTag(md: Modifier*) = {
    div(form, pendingContainer)
  }

  update()
}
