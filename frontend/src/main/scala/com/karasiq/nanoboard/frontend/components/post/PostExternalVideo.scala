package com.karasiq.nanoboard.frontend.components.post

import rx._
import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.Icons
import com.karasiq.videojs.{VideoJSBuilder, VideoSource}

private[components] object PostExternalVideo {
  def defaultType = "webm"

  def apply(url: String, sources: VideoSource*): PostExternalVideo = {
    new PostExternalVideo(url, sources)
  }

  def youtube(url: String): PostExternalVideo = {
    new PostExternalVideo(url, Seq(VideoSource("video/youtube", url)), Seq("youtube"))
  }
}

private[components] final class PostExternalVideo(url: String, sources: Seq[VideoSource], techOrder: Seq[String] = Nil) extends BootstrapHtmlComponent {
  val expanded = Var(false)

  private val videoPlayer = Rx[Frag] {
    if (expanded()) {
      VideoJSBuilder()
        .techOrder(techOrder:_*)
        .sources(sources:_*)
        .dimensions(640, 360)
        .fluid(true)
        .autoplay(true)
        .loop(true)
        .controls(true)
        .options("iv_load_policy" → 1)
        .build()
    } else {
      ""
    }
  }

  override def renderTag(md: Modifier*) = {
    span(
      a(href := url, fontWeight.bold, Icons.video, url, onclick := Callback.onClick(_ ⇒ expanded() = !expanded.now)),
      videoPlayer,
      md
    )
  }
}
