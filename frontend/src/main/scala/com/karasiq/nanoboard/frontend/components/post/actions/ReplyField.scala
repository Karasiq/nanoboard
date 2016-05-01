package com.karasiq.nanoboard.frontend.components.post.actions

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons._
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Blobs, CancelledException, Notifications}
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.taboverridejs.TabOverride
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.html.TextArea
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[post] object ReplyField {
  def apply(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): ReplyField = {
    new ReplyField(post)
  }

  def tabOverride: Modifier = new Modifier {
    override def applyTo(t: Element): Unit = {
      TabOverride.set(t.asInstanceOf[TextArea])
    }
  }
}

private[post] final class ReplyField(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Span] {
  import controller.{locale, style}

  val expanded = Var(false)

  val replyText = Var("")

  val lengthIsValid = replyText.map(text ⇒ (1 to 65535).contains(text.length))

  override def renderTag(md: Modifier*) = {
    val field = Form(
      FormInput.textArea((), style.input, placeholder := locale.writeYourMessage, rows := 5, replyText.reactiveInput, "has-errors".classIf(lengthIsValid.map(!_)), ReplyField.tabOverride)
    )

    val imageLink = Button(ButtonStyle.primary)(Icons.image, locale.insertImage, onclick := Bootstrap.jsClick { _ ⇒
      ImageAttachDialog().generate().onComplete {
        case Success(ImageData(base64, format)) ⇒
          val data = if (format == "svg+xml") "[img type=\"svg+xml\"]" + base64 + "[/img]" else s"[img=$base64]"
          replyText() = s"${replyText.now}$data"

        case Failure(CancelledException) ⇒
          // Pass

        case Failure(exc) ⇒
          Notifications.error(exc)(locale.attachmentGenerationError, Layout.topRight)
      }
    })

    val fileLink = Button(ButtonStyle.info)(Icons.file, locale.file, onclick := Bootstrap.jsClick { _ ⇒
      val field = input(`type` := "file", onchange := Bootstrap.jsInput { field ⇒
        val file = field.files.head
        Blobs.asBase64(file).foreach { base64 ⇒
          replyText() = s"${replyText.now}${if (replyText.now.nonEmpty) "\n" else ""}[file name=${'"' + file.name + '"'} type=${'"' + file.`type` + '"'}]$base64[/file]"
        }
      }).render
      field.click()
    })

    val submitButton = Button(ButtonStyle.success)("disabled".classIf(lengthIsValid.map(!_)), Icons.submit, locale.submit, onclick := Bootstrap.jsClick { _ ⇒
      if (lengthIsValid.now) {
        NanoboardApi.addReply(post.hash, replyText.now).onComplete {
          case Success(newPost) ⇒
            expanded() = false
            replyText() = ""
            controller.addPost(newPost)

          case Failure(exc) ⇒
            Notifications.error(exc)(locale.postingError, Layout.topRight)
        }
      }
    })

    val postLength = Rx {
      span(float.right, fontStyle.italic, if (!lengthIsValid()) color.red else (), s"${replyText().length} ${locale.bytes}")
    }

    span(
      // Reply link
      a(style.postLink, href := "#", Icons.reply, locale.reply, onclick := Bootstrap.jsClick { _ ⇒
        expanded() = !expanded.now
      }),

      // Input field
      div(field,
        ButtonGroup(ButtonGroupSize.small, imageLink, fileLink, submitButton),
        postLength,
        expanded.reactiveShow
      ),

      // Preview
      div(marginTop := 20.px, style.post, Rx(span(style.postInner, BoardStyle.Common.flatScroll, NanoboardPost.render(replyText()))), Rx(expanded() && replyText().nonEmpty).reactiveShow),

      md
    )
  }
}
