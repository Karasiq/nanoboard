package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Notifications, PostParser}
import com.karasiq.nanoboard.frontend.{Icons, NanoboardContext, NanoboardController}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

private[components] object NanoboardPost {
  def render(text: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): Frag = {
    PostRenderer().render(PostParser.parse(text))
  }

  def apply(showParent: Boolean, showAnswers: Boolean, data: NanoboardMessageData, scrollable: Boolean = false)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): NanoboardPost = {
    new NanoboardPost(showParent, showAnswers, data, scrollable)
  }
}

private[components] final class NanoboardPost(showParent: Boolean, showAnswers: Boolean, data: NanoboardMessageData, scrollable: Boolean)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
  import controller.{locale, style}

  val showSource = Var(false)

  override def renderTag(md: Modifier*): RenderedTag = {
    div(
      if (scrollable) id := s"post-${data.hash}" else (),
      style.post,
      div(
        style.postInner,
        span(
          style.postId,
          if (showParent && data.parent.isDefined) PostLink(data.parent.get).renderTag(Icons.parent) else (),
          sup(data.containerId.fold(data.hash)(cid ⇒ s"${data.hash}/$cid"))
        ),
        Rx[Frag](if (showSource()) data.text else span(NanoboardPost.render(data.text)))
      ),
      div(
        if (showAnswers && data.answers > 0) a(style.postLink, href := s"#${data.hash}", Icons.answers, s"${data.answers}", onclick := Bootstrap.jsClick {_ ⇒
          this.openAsThread()
        }) else (),
        a(style.postLink, href := "#", Icons.delete, locale.delete, onclick := Bootstrap.jsClick { _ ⇒
          this.delete()
        }),
        controller.isPending(data.hash).map { pending ⇒
          if (!pending) a(style.postLink, href := "#", Icons.enqueue, locale.enqueue, onclick := Bootstrap.jsClick { a ⇒
            NanoboardApi.markAsPending(data.hash).foreach { _ ⇒
              controller.addPending(data)
            }
          }) else a(style.postLink, href := "#", Icons.dequeue, locale.dequeue, onclick := Bootstrap.jsClick { a ⇒
            NanoboardApi.markAsNotPending(data.hash).foreach { _ ⇒
              controller.deletePending(data)
            }
          })
        },
        a(style.postLink, href := "#", Icons.source, locale.source, onclick := Bootstrap.jsClick(_ ⇒ showSource() = !showSource.now)),
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
      NanoboardApi.delete(data.hash).foreach { hashes ⇒
        controller.deleteSingle(data)
        hashes.foreach { hash ⇒
          controller.deleteSingle(NanoboardMessageData(None, None, hash, "", 0))
        }
      }
    }
  }
}
