package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.NanoboardMessageData
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import org.parboiled2.ParseError
import org.scalajs.dom
import rx.Ctx

import scala.util.{Failure, Success}
import scalatags.JsDom.all._

//noinspection VariablePatternShadow
// TODO: Formatting
final class NanoboardPost(isOp: Boolean, style: BoardStyle, data: NanoboardMessageData)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Div] {
  private def parsePost(text: String): Frag = {
    val parser = new NanoboardPostParser(text)
    val renderer = new NanoboardPostRenderer(style)
    parser.Message.run() match {
      case Success(value) ⇒
        renderer.render(value)

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
        span(parsePost(data.text), whiteSpace.pre)
      ),
      div(
        if (!isOp && data.answers > 0) a(style.postLink, href := s"#${data.hash}", "envelope-o".fontAwesome(FontAwesome.fixedWidth), s"${data.answers}") else (),
        a(style.postLink, href := "#", "trash-o".fontAwesome(FontAwesome.fixedWidth), "Delete", onclick := Bootstrap.jsClick(_ ⇒ ()))
      )
    )
  }
}
