package com.karasiq.nanoboard.sources.png

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.karasiq.nanoboard.encoding.DataEncodingStage
import org.jsoup.nodes.Element

final class SosachPngSource(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer) extends BoardPngSource(encoding) {
  private val regex = """https?://2ch\.hk/(\w+/src/\d+/\d+\.(?:png|bmp))""".r

  override protected def getUrl(e: Element, attr: String): Option[String] = {
    e.attr(attr) match {
      case regex(path) ⇒
        Some(s"http://m2-ch.ru/$path")

      case _ ⇒
        None
    }
  }

  override def imagesFromPage(url: String): Source[String, akka.NotUsed] = {
    super.imagesFromPage(url.replace("https://2ch.hk/", "http://m2-ch.ru/"))
  }
}
