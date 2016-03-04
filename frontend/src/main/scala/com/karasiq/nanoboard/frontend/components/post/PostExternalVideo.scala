package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.videojs.{VideoJSBuilder, VideoSource}
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._

private[components] object PostExternalVideo {
  def apply(url: String, sources: VideoSource*)(implicit ctx: Ctx.Owner): PostExternalVideo = {
    assert(sources.nonEmpty, "Video sources is empty")
    new PostExternalVideo(url, sources)
  }

  def youtube(url: String)(implicit ctx: Ctx.Owner): PostExternalVideo = {
    new PostExternalVideo(url, Seq(VideoSource("video/youtube", url)), Seq("youtube"))
  }
}

private[components] final class PostExternalVideo(url: String, sources: Seq[VideoSource], techOrder: Seq[String] = Nil)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Span] {
  val state = Var(false)

  private val videoPlayer = Rx[Frag] {
    if (state()) {
      VideoJSBuilder()
        .techOrder(techOrder:_*)
        .sources(sources:_*)
        .dimensions(640, 360)
        .autoplay(true)
        .loop(true)
        .controls(true)
        .build()
    } else {
      ""
    }
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(href := url, fontWeight.bold, "play-circle".fontAwesome(FontAwesome.fixedWidth), url, onclick := Bootstrap.jsClick(_ ⇒ state() = !state.now)),
      videoPlayer,
      md
    )
  }
}
