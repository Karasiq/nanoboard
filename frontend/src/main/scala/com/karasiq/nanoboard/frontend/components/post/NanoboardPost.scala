package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.NanoboardThread
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.utils.PostParser
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardContext, NanoboardMessageData}
import org.parboiled2.ParseError
import org.scalajs.dom
import rx.Ctx

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

//noinspection VariablePatternShadow
private[components] final class NanoboardPost(thread: NanoboardThread, isOp: Boolean, style: BoardStyle, data: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext) extends BootstrapHtmlComponent[dom.html.Div] {
  private def parsePost(text: String): Frag = {
    val parser = new PostParser(text)
    parser.Message.run() match {
      case Success(value) ⇒
        PostRenderer(style).render(value)

      case Failure(exc: ParseError) ⇒
        println(parser.formatError(exc))
        text

      case _ ⇒
        text
    }
  }

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
        span(parsePost(data.text))
      ),
      div(
        if (!isOp && data.answers > 0) a(style.postLink, href := s"#${data.hash}", "envelope-o".fontAwesome(FontAwesome.fixedWidth), s"${data.answers}", onclick := Bootstrap.jsClick {_ ⇒
          this.openAsThread()
        }) else (),
        a(style.postLink, href := "#", "trash-o".fontAwesome(FontAwesome.fixedWidth), "Delete", onclick := Bootstrap.jsClick { _ ⇒
          this.delete()
        })
      ),
      md
    )
  }

  def openAsThread(): Unit = {
    thread.context() = NanoboardContext.Thread(data.hash, 0)
  }

  // TODO: Confirmation
  def delete(): Unit = {
    NanoboardApi.delete(data.hash).foreach { _ ⇒
      thread.context.now match {
        case NanoboardContext.Thread(data.hash, _) ⇒
          thread.context() = data.parent.filterNot(_.forall(_ == '0'))
            .fold[NanoboardContext](NanoboardContext.Root)(NanoboardContext.Thread(_, 0))

        case _ ⇒
          thread.update()
      }
    }
  }
}