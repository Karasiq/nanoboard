package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.NanoboardController
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardMessageData}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

final class PostReplyField(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Span] {
  import controller.style

  val expanded = Var(false)

  val replyText = Var("")

  private val preview = Rx {
    span(style.postInner, NanoboardPost.render(replyText()))
  }

  private val field = {
    Form(
      FormInput.textArea("Message", placeholder := "Write your message", rows := 5, replyText.reactiveInput),
      Form.submit("Submit"),
      onsubmit := Bootstrap.jsSubmit { _ ⇒
        NanoboardApi.addReply(post.hash, replyText.now)
          .foreach { newPost ⇒
            expanded() = false
            replyText() = ""
            controller.addPost(newPost)
          }
      }
    )
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(style.postLink, href := "#", "reply".fontAwesome(FontAwesome.fixedWidth), "Reply", onclick := Bootstrap.jsClick { _ ⇒
        expanded() = !expanded.now
      }),
      div(field, expanded.reactiveShow),
      div(style.post, preview, expanded.reactiveShow),
      md
    )
  }
}
