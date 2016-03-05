package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.NanoboardController
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Notifications, PostParser}
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardContext, NanoboardMessageData}
import org.parboiled2.ParseError
import org.scalajs.dom
import rx.Ctx

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[components] object NanoboardPost {
  def render(text: String)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): Frag = {
    val parser = new PostParser(text)
    parser.Message.run() match {
      case Success(value) ⇒
        PostRenderer().render(value)

      case Failure(exc: ParseError) ⇒
        println(parser.formatError(exc))
        text

      case _ ⇒
        text
    }
  }

  def apply(isOp: Boolean, data: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): NanoboardPost = {
    new NanoboardPost(isOp, data)
  }
}

private[components] final class NanoboardPost(isOp: Boolean, data: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
  import controller.style

  override def renderTag(md: Modifier*): RenderedTag = {
    div(
      style.post,
      div(
        style.postInner,
        span(
          style.postId,
          if (isOp && data.parent.isDefined) a(href := s"#${data.parent.mkString}", "level-up".fontAwesome(FontAwesome.fixedWidth)) else (),
          sup(data.hash)
        ),
        span(NanoboardPost.render(data.text))
      ),
      div(
        if (!isOp && data.answers > 0) a(style.postLink, href := s"#${data.hash}", "envelope-o".fontAwesome(FontAwesome.fixedWidth), s"${data.answers}", onclick := Bootstrap.jsClick {_ ⇒
          this.openAsThread()
        }) else (),
        a(style.postLink, href := "#", "trash-o".fontAwesome(FontAwesome.fixedWidth), "Delete", onclick := Bootstrap.jsClick { _ ⇒
          this.delete()
        }),
        PostReplyField(data)
      ),
      md
    )
  }

  def openAsThread(): Unit = {
    controller.setContext(NanoboardContext.Thread(data.hash, 0))
  }

  def delete(): Unit = {
    Notifications.confirmation(s"Delete post #${data.hash}?", Layout.topLeft) {
      NanoboardApi.delete(data.hash).foreach { _ ⇒
        controller.deletePost(data)
      }
    }
  }
}
