package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{ButtonBuilder, ButtonGroup, ButtonGroupSize, ButtonStyle}
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.{NanoboardApi, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{CancelledException, Notifications}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[components] object PostReplyField {
  def apply(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): PostReplyField = {
    new PostReplyField(post)
  }
}

private[components] final class PostReplyField(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Span] {
  import controller.style

  val expanded = Var(false)

  val replyText = Var("")

  val lengthIsValid = replyText.map(text ⇒ (1 to 65535).contains(text.length))

  override def renderTag(md: Modifier*) = {
    val field = Form(
      FormInput.textArea((), placeholder := "Write your message", rows := 5, replyText.reactiveInput, "has-errors".classIf(lengthIsValid.map(!_)))
    )

    val attachmentLink = ButtonBuilder(ButtonStyle.primary)("file-image-o".fontAwesome(FontAwesome.fixedWidth), "Insert image", onclick := Bootstrap.jsClick { _ ⇒
      AttachmentGenerationDialog().generate().onComplete {
        case Success(base64) ⇒
          replyText() = replyText.now + s"[img=$base64]"

        case Failure(CancelledException) ⇒
          // Pass

        case Failure(exc) ⇒
          Notifications.error(exc)("Attachment generation error", Layout.topRight)
      }
    })

    val submitButton = ButtonBuilder(ButtonStyle.success)("disabled".classIf(lengthIsValid.map(!_)), "mail-forward".fontAwesome(FontAwesome.fixedWidth), "Submit", onclick := Bootstrap.jsClick { _ ⇒
      if (lengthIsValid.now) {
        NanoboardApi.addReply(post.hash, replyText.now).onComplete {
          case Success(newPost) ⇒
            expanded() = false
            replyText() = ""
            controller.addPost(newPost)

          case Failure(exc) ⇒
            Notifications.error(exc)("Posting error", Layout.topRight)
        }
      }
    })

    val postLength = Rx {
      span(float.right, fontStyle.italic, if (!lengthIsValid()) color.red else (), s"${replyText().length} bytes")
    }

    span(
      // Reply link
      a(style.postLink, href := "#", "reply".fontAwesome(FontAwesome.fixedWidth), "Reply", onclick := Bootstrap.jsClick { _ ⇒
        expanded() = !expanded.now
      }),

      // Input field
      div(field,
        ButtonGroup(ButtonGroupSize.default, attachmentLink, submitButton),
        postLength,
        expanded.reactiveShow
      ),

      // Preview
      div(marginTop := 20.px, style.post, Rx(span(style.postInner, NanoboardPost.render(replyText()))), Rx(expanded() && replyText().nonEmpty).reactiveShow),

      md
    )
  }
}
