package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import com.karasiq.nanoboard.frontend.utils._
import rx._

import scalatags.JsDom.all._

//noinspection VariablePatternShadow
private[components] object PostRenderer {
  def apply(style: BoardStyle)(implicit ctx: Ctx.Owner): PostRenderer = {
    new PostRenderer(style)
  }

  def asPlainText(parsed: PostDomValue): String = parsed match {
    case PlainText(value) ⇒
      value

    case PostDomValues(values) ⇒
      values.map(asPlainText).mkString

    case BoldText(value) ⇒
      asPlainText(value)

    case ItalicText(value) ⇒
      asPlainText(value)

    case UnderlinedText(value) ⇒
      asPlainText(value)

    case StrikeThroughText(value) ⇒
      asPlainText(value)

    case GreenText(_) | SpoilerText(_) | InlineImage(_) | FractalMusic(_) ⇒
      ""

    case ExternalImage(url) ⇒
      url

    case ExternalVideo(url) ⇒
      url
  }
}

//noinspection VariablePatternShadow
private[components] final class PostRenderer(style: BoardStyle)(implicit ctx: Ctx.Owner) {
  def render(parsed: PostDomValue): Frag = parsed match {
    case PlainText(value) ⇒
      Linkifier(value)

    case PostDomValues(values) ⇒
      values.map(render)

    case BoldText(value) ⇒
      span(fontWeight.bold, render(value))

    case ItalicText(value) ⇒
      span(fontStyle.italic, render(value))

    case UnderlinedText(value) ⇒
      span(textDecoration.underline, render(value))

    case StrikeThroughText(value) ⇒
      span(textDecoration.`line-through`, render(value))

    case GreenText(value) ⇒
      span(style.greenText, render(value))

    case SpoilerText(value) ⇒
      span(style.spoiler, render(value))

    case InlineImage(base64) ⇒
      PostInlineImage(base64)

    case ExternalImage(url) ⇒
      PostExternalImage(url)

    case ExternalVideo(url) ⇒
      PostExternalVideo(url)

    case FractalMusic(music) ⇒
      s"<music: $music>"
  }
}
