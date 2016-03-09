package com.karasiq.nanoboard.sources.png

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.DataEncodingStage
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConversions._
import scala.util.Try

class BoardPngSource(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer) extends UrlPngSource {
  protected final val http = Http()

  def messagesFromImage(url: String): Source[NanoboardMessage, akka.NotUsed] = {
    Source.fromFuture(http.singleRequest(HttpRequest(uri = url)))
      .flatMapConcat(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _))
      .mapConcat { data ⇒
        val decoded: String = encoding.decode(data).utf8String
        NanoboardMessage.parseMessages(decoded)
      }
      .recoverWith { case _ ⇒ Source.empty }
  }

  def imagesFromPage(url: String): Source[String, akka.NotUsed] = {
    Source.fromFuture(http.singleRequest(HttpRequest(uri = url)))
      .flatMapConcat(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _))
      .flatMapConcat(data ⇒ imagesFromPage(Jsoup.parse(data.utf8String, url)))
      .recoverWith { case _ ⇒ Source.empty }
  }

  protected def imagesFromPage(page: Document): Source[String, akka.NotUsed] = {
    val urls = page.select("a").flatMap(getUrl(_, "href"))
    Source(urls.toVector)
      .log("page-image")
  }

  protected def getUrl(e: Element, attr: String): Option[String] = {
    Try(new URL(e.absUrl(attr)))
      .toOption
      .filter(_.getPath.matches("([/\\w]+)?/src/([/\\w]+)?\\.(png|bmp)"))
      .map(_.toString)
  }
}