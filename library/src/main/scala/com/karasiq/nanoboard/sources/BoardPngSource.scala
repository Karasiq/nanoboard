package com.karasiq.nanoboard.sources

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.DataEncodingStage
import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.typesafe.config.{Config, ConfigFactory}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConversions._

object BoardPngSource {
  def fromConfig(config: Config)(implicit as: ActorSystem, am: ActorMaterializer): BoardPngSource = {
    new BoardPngSource(Seq(GzipCompression(), SalsaCipher(config.getString("encryption-key")), PngEncoding.decoder))
  }

  def apply(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer): BoardPngSource = {
    new BoardPngSource(encoding)
  }

  def apply()(implicit as: ActorSystem, am: ActorMaterializer): BoardPngSource = {
    fromConfig(ConfigFactory.load().getConfig("nanoboard"))
  }
}

class BoardPngSource(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer) {
  protected val http = Http()

  def messagesFromImage(url: String): Source[NanoboardMessage, akka.NotUsed] = {
    Source.fromFuture(http.singleRequest(HttpRequest(uri = url)))
      .flatMapConcat(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _))
      .map { data ⇒
        val decoded: String = encoding.decode(data).utf8String
        NanoboardMessage.parseMessages(decoded)
      }
      .recover { case _ ⇒ Nil }
      .filter(_.nonEmpty)
      .log("image-messages")
      .mapConcat(identity)
  }

  def imagesFromPage(url: String): Source[String, akka.NotUsed] = {
    Source.fromFuture(http.singleRequest(HttpRequest(uri = url)))
      .flatMapConcat(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _))
      .map(data ⇒ imagesFromPage(Jsoup.parse(data.utf8String, url)))
      .recover { case _ ⇒ Source.empty }
      .flatMapConcat(identity)
  }

  protected def imagesFromPage(page: Document): Source[String, akka.NotUsed] = {
    val urls = page.select("img").flatMap { img ⇒
      def getUrl(e: Element, attr: String): Option[String] = {
        val url: String = e.attr(attr)
        if (url.startsWith("/src/") && (url.endsWith(".png") || url.endsWith(".bmp"))) {
          Some(e.absUrl(attr))
        } else {
          None
        }
      }

      Vector(getUrl(img.parent(), "href"), getUrl(img, "src"))
        .flatten
        .headOption
    }

    Source(urls.toVector)
      .log("page-image")
  }
}
