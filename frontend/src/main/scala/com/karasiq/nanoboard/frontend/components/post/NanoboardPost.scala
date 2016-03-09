package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.api.{NanoboardApi, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Notifications, PostParser}
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import org.scalajs.dom
import rx.Ctx

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

private[components] object NanoboardPost {
  def render(text: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): Frag = {
    PostRenderer().render(PostParser.parse(text))
  }

  def apply(showParent: Boolean, showAnswers: Boolean, data: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): NanoboardPost = {
    new NanoboardPost(showParent, showAnswers, data)
  }
}

private[components] final class NanoboardPost(showParent: Boolean, showAnswers: Boolean, data: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
  import controller.{locale, style}

  override def renderTag(md: Modifier*): RenderedTag = {
    div(
      style.post,
      div(
        style.postInner,
        span(
          style.postId,
          if (showParent && data.parent.isDefined) PostLink(data.parent.get).renderTag("level-up".fontAwesome(FontAwesome.fixedWidth)) else (),
          sup(data.hash)
        ),
        span(NanoboardPost.render(data.text))
      ),
      div(
        if (showAnswers && data.answers > 0) a(style.postLink, href := s"#${data.hash}", "envelope-o".fontAwesome(FontAwesome.fixedWidth), s"${data.answers}", onclick := Bootstrap.jsClick {_ ⇒
          this.openAsThread()
        }) else (),
        a(style.postLink, href := "#", "trash-o".fontAwesome(FontAwesome.fixedWidth), locale.delete, onclick := Bootstrap.jsClick { _ ⇒
          this.delete()
        }),
        controller.isPending(data.hash).map { pending ⇒
          if (!pending) a(style.postLink, href := "#", "sign-in".fontAwesome(FontAwesome.fixedWidth), locale.enqueue, onclick := Bootstrap.jsClick { a ⇒
            NanoboardApi.markAsPending(data.hash).foreach { _ ⇒
              controller.addPending(data)
            }
          }) else a(style.postLink, href := "#", "sign-out".fontAwesome(FontAwesome.fixedWidth), locale.dequeue, onclick := Bootstrap.jsClick { a ⇒
            NanoboardApi.markAsNotPending(data.hash).foreach { _ ⇒
              controller.deletePending(data)
            }
          })
        },
        PostReplyField(data)
      ),
      md
    )
  }

  def openAsThread(): Unit = {
    controller.setContext(NanoboardContext.Thread(data.hash, 0))
  }

  def delete(): Unit = {
    Notifications.confirmation(locale.deleteConfirmation(data.hash), Layout.topLeft) {
      NanoboardApi.delete(data.hash).foreach { _ ⇒
        controller.deletePost(data)
      }
    }
  }
}
