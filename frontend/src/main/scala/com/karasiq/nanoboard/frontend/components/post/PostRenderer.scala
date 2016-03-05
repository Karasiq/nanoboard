package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.components.NanoboardController
import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import com.karasiq.nanoboard.frontend.utils._
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

//noinspection VariablePatternShadow
private[components] object PostRenderer {
  def apply()(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): PostRenderer = {
    new PostRenderer
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
private[components] final class PostRenderer(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) {
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
      span(controller.style.greenText, render(value))

    case SpoilerText(value) ⇒
      span(controller.style.spoiler, render(value))

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
