package com.karasiq.nanoboard.sources.bitmessage

import java.nio.charset.StandardCharsets

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._

import com.karasiq.nanoboard.NanoboardMessage

object BitMessageTransport {
  def fromConfig(bmConfig: Config)(implicit ac: ActorSystem, am: ActorMaterializer) = {
    val chanAddress = bmConfig.getString("chan-address")
    val apiAddress = bmConfig.getString("host")
    val apiPort = bmConfig.getInt("port")
    val apiUsername = bmConfig.getString("username")
    val apiPassword = bmConfig.getString("password")
    new BitMessageTransport(chanAddress, apiAddress, apiPort, apiUsername, apiPassword)
  }

  def apply(config: Config = ConfigFactory.load())(implicit ac: ActorSystem, am: ActorMaterializer) = {
    fromConfig(config.getConfig("nanoboard.bitmessage"))
  }

  def wrap(messages: NanoboardMessage*): String = {
    val wrappedMessages = messages.map(m ⇒ WrappedNanoboardMessage(m.hash, asBase64(NanoboardMessage.textWithSignatureTags(m)), m.parent))
    Json.toJson(wrappedMessages).toString()
  }

  def unwrap(bitMessage: String): Vector[NanoboardMessage] = {
    val messages = Json.parse(bitMessage).as[Vector[WrappedNanoboardMessage]]
    messages.map { wrapped ⇒
      val (text, pow, signature) = NanoboardMessage.stripSignatureTags(fromBase64(wrapped.message))
      NanoboardMessage(wrapped.replyTo, text, pow.getOrElse(NanoboardMessage.NoPOW), signature.getOrElse(NanoboardMessage.NoSignature))
    }
  }

  @inline
  private[bitmessage] def asBase64(string: String): String = {
    Base64.encodeBase64String(string.getBytes(StandardCharsets.UTF_8))
  }

  @inline
  private[bitmessage] def fromBase64(string: String): String = {
    new String(Base64.decodeBase64(string), StandardCharsets.UTF_8)
  }
}

/**
  * Nanoboard BitMessage transport, compatible with official implementation.
  * @see [[https://github.com/nanoboard/nanoboard-bittransport]]
  */
final class BitMessageTransport(chanAddress: String, apiAddress: String, apiPort: Int, apiUsername: String, apiPassword: String)(implicit ac: ActorSystem, am: ActorMaterializer) {
  import XmlRpcProxy._
  private val http = Http()
  private val xmlRpcProxy = new XmlRpcProxy(http, apiAddress, apiPort, apiUsername, apiPassword)

  def sendMessage(message: NanoboardMessage): Future[HttpResponse] = {
    xmlRpcProxy.sendMessage(chanAddress, chanAddress, (), BitMessageTransport.asBase64(BitMessageTransport.wrap(message)), 2, 21600)
  }

  def receiveMessages(host: String, port: Int, sink: Sink[NanoboardMessage, _]): Future[Http.ServerBinding] = {
    http.bindAndHandle(route(sink), host, port)
  }

  private def route(sink: Sink[NanoboardMessage, _]) = {
    val queue = Source
      .queue(20, OverflowStrategy.dropHead)
      .to(sink)
      .run()

    post {
      (path("api" / "add" / NanoboardMessage.HashFormat) & entity(as[String])) { (parent, message) ⇒
        val (text, pow, signature) = NanoboardMessage.stripSignatureTags(BitMessageTransport.fromBase64(message))
        queue.offer(NanoboardMessage(parent, text, pow.getOrElse(NanoboardMessage.NoPOW), signature.getOrElse(NanoboardMessage.NoSignature)))
        complete(StatusCodes.OK)
      }
    }
  }
}
