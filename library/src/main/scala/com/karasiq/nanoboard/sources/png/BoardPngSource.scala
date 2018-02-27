package com.karasiq.nanoboard.sources.png

import java.net.{InetSocketAddress, URI}

import scala.collection.JavaConversions._
import scala.util.Try

import akka.actor.ActorSystem
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.DataEncodingStage

private object BoardPngSource {
  def createHttpSettings(config: Config) = {
    val settings = ConnectionPoolSettings(config)
    val proxy = Try(config.getString("nanoboard.proxy")).toOption
    proxy match {
      case Some(proxy) if proxy.contains(":") ⇒
        val Array(host, port) = proxy.split(":", 2)
        settings.withTransport(ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(host, port.toInt)))

      case Some(proxy) if proxy.nonEmpty ⇒
        settings.withTransport(ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxy, 8080)))

      case _ ⇒
        settings
    }
  }
}

/**
  * Generic imageboard PNG downloader
  * @param encoding PNG data decoder
  */
class BoardPngSource(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer) extends UrlPngSource {
  protected final val http = Http()
  protected val httpSettings = BoardPngSource.createHttpSettings(as.settings.config)

  def messagesFromImage(url: String): Source[NanoboardMessage, akka.NotUsed] = {
    Source.fromFuture(http.singleRequest(HttpRequest(uri = url), settings = httpSettings))
      .flatMapConcat(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _))
      .mapConcat { data ⇒
        // println(encoding.decode(data).utf8String)
        NanoboardMessage.parseMessages(encoding.decode(data))
      }
      .recoverWithRetries(1, { case _ ⇒ Source.empty })
      .named("boardImageMessages")
  }

  def imagesFromPage(url: String): Source[String, akka.NotUsed] = {
    Source.fromFuture(http.singleRequest(HttpRequest(uri = url), settings = httpSettings))
      .flatMapConcat(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _))
      .flatMapConcat(data ⇒ imagesFromPage(Jsoup.parse(data.utf8String, url)))
      .recoverWithRetries(1, { case _ ⇒ Source.empty })
      .named("boardImages")
  }

  protected def imagesFromPage(page: Document): Source[String, akka.NotUsed] = {
    val urls = page.select("a").flatMap(getUrl(_, "href"))
    Source(urls.distinct.toVector)
  }

  protected def getUrl(e: Element, attr: String): Option[String] = {
    Try(new URI(e.absUrl(attr)))
      .toOption
      .filter(_.getPath.toLowerCase.endsWith(".png")) // .filter(_.getPath.matches("([^\\?\\s]+)?/src/([^\\?\\s]+)?\\.png"))
      .map(_.toString)
  }
}