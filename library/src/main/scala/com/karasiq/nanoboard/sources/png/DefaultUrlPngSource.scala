package com.karasiq.nanoboard.sources.png

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.karasiq.nanoboard.encoding.DataEncodingStage

final class DefaultUrlPngSource(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer) extends UrlPngSource {
  private val generic = new BoardPngSource(encoding)
  private val sosach = new SosachPngSource(encoding)

  private def select(url: String): UrlPngSource = {
    if (url.startsWith("https://2ch.hk/")) sosach
    else generic
  }

  override def messagesFromImage(url: String) = {
    select(url).messagesFromImage(url)
  }

  override def imagesFromPage(url: String) = {
    select(url).imagesFromPage(url)
  }
}
