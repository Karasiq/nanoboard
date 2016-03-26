package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import com.karasiq.nanoboard.frontend.utils._
import com.karasiq.videojs.VideoSource
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scalatags.JsDom.all._

private[components] object PostRenderer {
  def apply()(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController): PostRenderer = {
    new PostRenderer
  }

  def renderMarkdown(source: String): Frag = {
    val renderer = js.Dynamic.newInstance(js.Dynamic.global.marked.Renderer)()
    renderer.image = { (url: String, imageTitle: String, text: String) ⇒
      import scalatags.Text.all._
      a(href := url, title := text, target := "_blank", imageTitle).render
    }
    renderer.code = { (source: String, language: String) ⇒
      import scalatags.Text.all.{source => _, _}
      val result = js.Dynamic.global.hljs.highlight(language, source).value.asInstanceOf[UndefOr[String]]
      span(whiteSpace.`pre-wrap`, code(`class` := s"hljs $language", result.fold[Modifier](source)(raw))).render
    }
    renderer.table = { (header: String, body: String) ⇒
      import scalatags.Text.all.{body => _, header => _, _}
      div(`class` := "table-responsive", table(`class` := "table", thead(raw(header)), tbody(raw(body)))).render
    }

    span(whiteSpace.normal, raw(js.Dynamic.global.marked(source, js.Dynamic.literal(
      renderer = renderer,
      gfm = true,
      tables = true,
      breaks = false,
      pedantic = false,
      sanitize = true,
      smartLists = true,
      smartypants = true
    )).asInstanceOf[String]))
  }

  def asPlainText(parsed: PostDomValue): String = parsed match {
    case PlainText(value) ⇒
      value

    case PostDomValues(values) ⇒
      values.map(asPlainText).mkString

    case BBCode("g" | "sp" | "spoiler", _) ⇒
      ""

    case BBCode(_, value) ⇒
      asPlainText(value)

    case _ ⇒
      ""
  }
}

// TODO: Fractal music
private[components] final class PostRenderer(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) {
  def render(parsed: PostDomValue): Frag = parsed match {
    case PlainText(value) ⇒
      Linkifier(value)

    case PostDomValues(values) ⇒
      values.map(render)

    case Markdown(value) ⇒
      PostRenderer.renderMarkdown(value)

    case BBCode("b", value) ⇒
      span(fontWeight.bold, render(value))

    case BBCode("i", value) ⇒
      span(fontStyle.italic, render(value))

    case BBCode("u", value) ⇒
      span(textDecoration.underline, render(value))

    case BBCode("s", value) ⇒
      span(textDecoration.`line-through`, render(value))

    case BBCode("g", value) ⇒
      span(controller.style.greenText, render(value))

    case BBCode("sp" | "spoiler", value) ⇒
      span(controller.style.spoiler, render(value))

    case ShortBBCode("img", base64) ⇒
      PostInlineImage(base64)

    case ShortBBCode("simg", url) ⇒
      PostExternalImage(url)

    case ShortBBCode("svid", url) ⇒
      PostExternalVideo(url, VideoSource("video/webm", url))

    case ShortBBCode("fm", music) ⇒
      s"<FM: $music>"

    // Unknown
    case BBCode(name, value) ⇒
      span(s"[$name]", render(value), s"[/$name]")

    case ShortBBCode(name, value) ⇒
      s"[$name=$value]"
  }
}
