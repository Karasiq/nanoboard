package com.karasiq.nanoboard.frontend.components.post.actions

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

import org.scalajs.dom.Element
import org.scalajs.dom.html.TextArea
import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.styles.CommonStyles
import com.karasiq.nanoboard.frontend.utils.{Blobs, CancelledException, Notifications}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.taboverridejs.TabOverride

private[post] object ReplyField {
  def apply(post: NanoboardMessageData)(implicit controller: NanoboardController): ReplyField = {
    new ReplyField(post)
  }

  def tabOverride: Modifier = new Modifier {
    override def applyTo(t: Element): Unit = {
      TabOverride.set(t.asInstanceOf[TextArea])
    }
  }
}

private[post] final class ReplyField(post: NanoboardMessageData)(implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  import controller.{locale, style}

  val expanded = Var(false)
  val replyText = Var("")
  val lengthIsValid = replyText.map(text ⇒ (1 to 65535).contains(text.length))

  override def renderTag(md: Modifier*) = {
    val field = Form(
      FormInput.textArea((), style.input, placeholder := locale.writeYourMessage, rows := 5, replyText.reactiveInput, "has-errors".classIf(lengthIsValid.map(!_)), ReplyField.tabOverride)
    )

    val imageLink = Button(ButtonStyle.primary)(Icons.image, locale.insertImage, onclick := Callback.onClick { _ ⇒
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

    val fileLink = Button(ButtonStyle.info)(Icons.file, locale.file, onclick := Callback.onClick { _ ⇒
      val field = input(`type` := "file", onchange := Callback.onInput { field ⇒
        val file = field.files.head
        Blobs.asBase64(file).foreach { base64 ⇒
          replyText() = s"${replyText.now}${if (replyText.now.nonEmpty) "\n" else ""}[file name=${'"' + file.name + '"'} type=${'"' + file.`type` + '"'}]$base64[/file]"
        }
      }).render
      field.click()
    })

    val submitButton = Button(ButtonStyle.success)(/* "disabled".classIf(lengthIsValid.map(!_)),*/ Icons.submit, locale.submit, onclick := Callback.onClick { _ ⇒
      if (/* lengthIsValid.now */ replyText.now.nonEmpty) {
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
      a(style.postLink, href := "#", Icons.reply, locale.reply, onclick := Callback.onClick { _ ⇒
        expanded() = !expanded.now
      }),

      // Input field
      div(field,
        ButtonGroup(ButtonGroupSize.small, imageLink, fileLink, submitButton),
        postLength,
        expanded.reactiveShow
      ),

      // Preview
      div(marginTop := 20.px, style.post, Rx(span(style.postInner, CommonStyles.flatScroll, NanoboardPost.render(replyText()))), Rx(expanded() && replyText().nonEmpty).reactiveShow),

      md
    )
  }
}
