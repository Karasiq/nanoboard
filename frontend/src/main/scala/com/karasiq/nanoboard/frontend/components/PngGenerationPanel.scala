package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.api.{NanoboardApi, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Blobs, Notifications}
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
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

final class PngGenerationPanel(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] with PostsContainer {
  import controller.locale

  override val posts = Var(Vector.empty[NanoboardMessageData])

  override val context: Var[NanoboardContext] = Var(NanoboardContext.Categories)

  private val loading = Var(false)

  override def addPost(post: NanoboardMessageData): Unit = {
    if (!posts.now.exists(_.hash == post.hash)) {
      posts() = posts.now :+ post
    }
  }

  override def deletePost(post: NanoboardMessageData): Unit = {
    val filtered = posts.now.filterNot(p ⇒ p.hash == post.hash || p.parent.contains(post.hash))
    posts() = filtered.collect {
      case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
        msg.copy(answers = answers - 1)

      case msg ⇒
        msg
    }
  }

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
      h3(locale.pendingPosts),
      for (p ← posts) yield GridSystem.mkRow(NanoboardPost(showParent = true, showAnswers = false, p))
    ) else ()
  }

  private val form = Form(
    FormInput.number(locale.pendingPosts, name := "pending", value := 10, min := 0),
    FormInput.number(locale.randomPosts, name := "random", value := 30, min := 0),
    FormInput.text(locale.imageFormat, name := "format", value := "png"),
    FormInput.file(locale.dataContainer, name := "container"),
    Form.submit(locale.generateContainer)("disabled".classIf(loading)),
    onsubmit := Bootstrap.jsSubmit { frm ⇒
      if (!loading.now) {
        loading() = true
        def input(name: String) = frm(name).asInstanceOf[Input]
        input("container").files.headOption match {
          case Some(file) ⇒
            val pending = input("pending").valueAsNumber
            val random = input("random").valueAsNumber
            val format: String = input("format").value

            NanoboardApi.generateContainer(pending, random, format, file).onComplete {
              case Success(blob) ⇒
                loading() = false
                Blobs.saveBlob(blob, s"${js.Date.now()}.$format")
                update()

              case Failure(exc) ⇒
                loading() = false
                Notifications.error(exc)(locale.containerGenerationError, Layout.topRight, 1500)
            }

          case None ⇒
            loading() = false
            Notifications.warning(locale.fileNotSelected, Layout.topRight)
        }
      }
    }
  )

  override def renderTag(md: Modifier*) = {
    div(form, pendingContainer)
  }

  update()
}
