package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{CancelledException, Notifications, PostParser}
import com.karasiq.nanoboard.frontend.{Icons, NanoboardContext, NanoboardController}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[components] object NanoboardPost {
  def render(text: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): Frag = {
    PostRenderer().render(PostParser.parse(text))
  }

  def apply(showParent: Boolean, showAnswers: Boolean, data: NanoboardMessageData, scrollable: Boolean = false)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): NanoboardPost = {
    new NanoboardPost(showParent, showAnswers, data, scrollable)
  }
}

private[components] final class NanoboardPost(showParent: Boolean, showAnswers: Boolean, postData: NanoboardMessageData, scrollable: Boolean)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
  import controller.{locale, style}

  val expanded = Var(false)
  val showSource = Var(false)

  override def renderTag(md: Modifier*): RenderedTag = {
    val heightMod = Rx[AutoModifier] {
      if (expanded())
        maxHeight := 100.pct
      else
        maxHeight := 48.em
    }

    val verified = Var(false)
    val verifyLoading = Var(false)
    div(
      if (scrollable) id := s"post-${postData.hash}" else (),
      style.post,
      div(
        heightMod,
        style.postInner,
        BoardStyle.Common.flatScroll,
        span(
          style.postId,
          if (showParent && postData.parent.isDefined) PostLink(postData.parent.get).renderTag(Icons.parent) else (),
          sup(cursor.pointer, postData.containerId.fold(postData.hash)(cid ⇒ s"${postData.hash}/$cid"), onclick := Bootstrap.jsClick(_ ⇒ expanded() = !expanded.now))
        ),
        Rx[Frag](if (showSource()) postData.textWithoutSign else span(NanoboardPost.render(postData.textWithoutSign)))
      ),
      div(
        if (showAnswers && postData.answers > 0) a(style.postLink, href := s"#${postData.hash}", Icons.answers, s"${postData.answers}", onclick := Bootstrap.jsClick { _ ⇒
          this.openAsThread()
        }) else (),
        a(style.postLink, href := "#", Icons.delete, locale.delete, onclick := Bootstrap.jsClick { _ ⇒
          this.delete()
        }),
        controller.isPending(postData.hash).map { pending ⇒
          if (!pending) a(style.postLink, href := "#", Icons.enqueue, locale.enqueue, onclick := Bootstrap.jsClick { a ⇒
            NanoboardApi.markAsPending(postData.hash).foreach { _ ⇒
              controller.addPending(postData)
            }
          }) else a(style.postLink, href := "#", Icons.dequeue, locale.dequeue, onclick := Bootstrap.jsClick { a ⇒
            NanoboardApi.markAsNotPending(postData.hash).foreach { _ ⇒
              controller.deletePending(postData)
            }
          })
        },
        a(style.postLink, href := "#", Icons.source, locale.source, onclick := Bootstrap.jsClick(_ ⇒ showSource() = !showSource.now)),
        a(style.postLink, href := "#", Icons.verify, locale.verify, Rx[AutoModifier](if (verifyLoading()) textDecoration.`line-through` else textDecoration.none), Rx[AutoModifier](if (verified() || postData.answers > 0 || postData.isSigned) display.none else display.inline), onclick := Bootstrap.jsClick { _ ⇒
          if (!verifyLoading.now) {
            verifyLoading() = true
            CaptchaDialog().verify(postData.hash).onComplete {
              case Success(newData) ⇒
                verifyLoading() = false
                verified() = true
                controller.addPending(postData) // Show as pending

              case Failure(CancelledException) ⇒
                verifyLoading() = false

              case Failure(exc) ⇒
                verifyLoading() = false
                Notifications.error(exc)(locale.verificationError, Layout.topRight)
            }
          }
        }),
        PostReplyField(postData)
      ),
      md
    )
  }

  def openAsThread(): Unit = {
    controller.setContext(NanoboardContext.Thread(postData.hash, 0))
  }

  def delete(): Unit = {
    Notifications.confirmation(locale.deleteConfirmation(postData.hash), Layout.topLeft) {
      NanoboardApi.delete(postData.hash).foreach { hashes ⇒
        controller.deleteSingle(postData)
        hashes.foreach { hash ⇒
          controller.deleteSingle(NanoboardMessageData(None, None, hash, "", 0))
        }
      }
    }
  }
}
