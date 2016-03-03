package com.karasiq.nanoboard.frontend.components

import com.karasiq.nanoboard.frontend.styles.BoardStyle
import rx._

import scalatags.JsDom.all._

//noinspection VariablePatternShadow
final class NanoboardPostRenderer(style: BoardStyle)(implicit ctx: Ctx.Owner) {
  def render(parsed: PostDomValue): Frag = parsed match {
    case PlainText(value) ⇒
      value

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
      new NanoboardPostInlineImage(base64).renderTag()

    case ExternalImage(url) ⇒
      a(href := url, s"[Image] $url")

    case ExternalVideo(url) ⇒
      a(href := url, s"[Video] $url")

    case FractalMusic(music) ⇒
      s"<music: $music>"
  }
}
