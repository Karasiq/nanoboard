package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardContext, NanoboardMessageData}
import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.URL
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scalatags.JsDom.all._

object PngGenerationPanel {
  def apply()(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): PngGenerationPanel = {
    new PngGenerationPanel
  }
}

final class PngGenerationPanel(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] with NanoboardPostContainer {
  override val posts = Var(Vector.empty[NanoboardMessageData])

  override val context: Var[NanoboardContext] = Var(NanoboardContext.Root)

  override def update(): Unit = {
    NanoboardApi.pending().foreach { posts ⇒
      this.posts() = posts
    }
  }

  private val pendingContainer = Rx[Frag] {
    val posts = this.posts()
    if (posts.nonEmpty) Bootstrap.well(
      marginTop := 20.px,
      h3("Pending posts"),
      for (p ← posts) yield NanoboardPost(true, p)
    ) else ()
  }

  private val form = Form(
    FormInput.number("Pending posts", name := "pending", value := 10),
    FormInput.number("Random posts", name := "random", value := 50),
    FormInput.text("Output format", name := "format", value := "png"),
    FormInput.file("Data container", name := "container"),
    Form.submit("Generate container image"),
    onsubmit := Bootstrap.jsSubmit { frm ⇒
      def input(name: String) = frm(name).asInstanceOf[Input]
      input("container").files.headOption.foreach { file ⇒
        val format: String = input("format").value
        NanoboardApi.generateContainer(input("pending").valueAsNumber, input("random").valueAsNumber, format, file)
          .foreach { blob ⇒
            val urlObject = js.Dynamic.global.window.URL.asInstanceOf[URL]
            val url = urlObject.createObjectURL(blob)
            val anchor = a(href := url, "download".attr := s"${js.Date.now()}.$format", target := "_blank").render
            anchor.click()
            urlObject.revokeObjectURL(url)
            update()
          }
      }
    }
  )

  override def renderTag(md: Modifier*) = {
    div(form, pendingContainer)
  }

  update()
}
