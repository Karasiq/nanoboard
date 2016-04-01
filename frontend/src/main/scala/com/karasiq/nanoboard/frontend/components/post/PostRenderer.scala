package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.highlightjs.HighlightJS
import com.karasiq.markedjs.{Marked, MarkedOptions, MarkedRenderer}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import com.karasiq.nanoboard.frontend.utils._
import com.karasiq.videojs.VideoSource
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scalatags.JsDom.all._

private[components] object PostRenderer {
  def apply()(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): PostRenderer = {
    new PostRenderer
  }

  // Returns HTML string
  def formatCode(source: String, language: Option[String]): String = {
    import scalatags.Text.all.{source => _, _}
    val result = language.fold(HighlightJS.highlightAuto(source))(HighlightJS.highlight(_, source))
    span(whiteSpace.`pre-wrap`, code(`class` := s"hljs ${result.language}", raw(result.value))).render
  }

  def renderMarkdown(source: String): Frag = {
    val renderer = MarkedRenderer(
      image = { (url: String, imageTitle: String, text: String) ⇒
        import scalatags.Text.all._
        a(href := url, title := text, target := "_blank", imageTitle).render
      },
      code = { (source: String, language: String) ⇒
        formatCode(source, if (js.isUndefined(language)) None else Some(language))
      },
      table = { (header: String, body: String) ⇒
        import scalatags.Text.all.{body => _, header => _, _}
        div(`class` := "table-responsive", table(`class` := "table", thead(raw(header)), tbody(raw(body)))).render
      }
    )

    val options = MarkedOptions(
      renderer = renderer,
      gfm = true,
      tables = true,
      breaks = true,
      pedantic = false,
      sanitize = true,
      smartLists = true,
      smartypants = true
    )

    span(whiteSpace.normal, raw(Marked(source, options)))
  }

  def asText(parsed: PostDomValue): String = parsed match {
    case PlainText(value) ⇒
      value

    case Markdown(value) ⇒
      s"[md]$value[/md]"

    case PostDomValues(values) ⇒
      values.map(asText).mkString

    case BBCode(name, parameters, value) ⇒
      s"[$name${if (parameters.isEmpty) "" else parameters.map(p ⇒ p._1 + "=\"" + p._2 + "\"").mkString(" ", " ", "")}]" + asText(value) + s"[/$name]"

    case ShortBBCode(name, value) ⇒
      s"[$name=$value]"
  }

  def strip(parsed: PostDomValue): String = parsed match {
    case PlainText(value) ⇒
      value

    case PostDomValues(values) ⇒
      values.map(strip).mkString

    case BBCode("g" | "sp" | "spoiler", _, _) ⇒
      ""

    case BBCode(_, _, value) ⇒
      strip(value)

    case _ ⇒
      ""
  }
}

private[components] final class PostRenderer(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) {
  def render(parsed: PostDomValue): Frag = parsed match {
    case PlainText(value) ⇒
      Linkifier(value)

    case PostDomValues(values) ⇒
      values.map(render)

    case Markdown(value) ⇒
      PostRenderer.renderMarkdown(value)

    case BBCode("b", _, value) ⇒
      span(fontWeight.bold, render(value))

    case BBCode("i", _, value) ⇒
      span(fontStyle.italic, render(value))

    case BBCode("u", _, value) ⇒
      span(textDecoration.underline, render(value))

    case BBCode("s", _, value) ⇒
      span(textDecoration.`line-through`, render(value))

    case BBCode("g", _, value) ⇒
      span(controller.style.greenText, render(value))

    case BBCode("sp" | "spoiler", _, value) ⇒
      span(controller.style.spoiler, render(value))

    case ShortBBCode("img", base64) ⇒
      PostInlineImage(base64)

    case BBCode("img" | "image", parameters, value) ⇒
      PostInlineImage(PostRenderer.asText(value), parameters.getOrElse("type", PostInlineImage.defaultType))

    case ShortBBCode("simg", url) ⇒
      PostExternalImage(url)

    case BBCode("vid" | "video", parameters, value) ⇒
      val url = PostRenderer.asText(value)
      PostExternalVideo(url, VideoSource(s"video/${parameters.getOrElse("type", PostExternalVideo.defaultType)}", url))

    case ShortBBCode("svid", url) ⇒
      PostExternalVideo(url, VideoSource(s"video/${PostExternalVideo.defaultType}", url))

    case ShortBBCode("fm", music) ⇒
      PostFractalMusic(music)

    case BBCode("code", parameters, source) ⇒
      span(raw(PostRenderer.formatCode(PostRenderer.asText(source), parameters.get("lang"))))

    case BBCode("file", parameters, value) ⇒
      PostInlineFile(parameters.getOrElse("name", controller.locale.file), PostRenderer.asText(value), parameters.getOrElse("type", ""))

    case BBCode("link", parameters, value) ⇒
      a(target := "_blank", href := parameters.getOrElse("url", PostRenderer.asText(value)), render(value))

    // Unknown
    case value ⇒
      PostRenderer.asText(value)
  }
}
