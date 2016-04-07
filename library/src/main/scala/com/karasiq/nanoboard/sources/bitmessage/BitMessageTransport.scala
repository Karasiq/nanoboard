package com.karasiq.nanoboard.sources.bitmessage

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.karasiq.nanoboard.NanoboardMessage
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.Charsets
import org.apache.commons.codec.binary.Base64
import upickle.default._

import scala.concurrent.Future

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

  @inline
  private[bitmessage] def asBase64(string: String): String = {
    Base64.encodeBase64String(string.getBytes(Charsets.UTF_8))
  }

  @inline
  private[bitmessage] def fromBase64(string: String): String = {
    new String(Base64.decodeBase64(string), Charsets.UTF_8)
  }

  def wrap(messages: NanoboardMessage*): String = {
    write(messages.map(message ⇒ WrappedNanoboardMessage(message.hash, asBase64(message.text), message.parent)))
  }

  def unwrap(bitMessage: String): Vector[NanoboardMessage] = {
    read[Vector[WrappedNanoboardMessage]](bitMessage)
      .map { wrapped ⇒ NanoboardMessage(wrapped.replyTo, fromBase64(wrapped.message)) }
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
      (path("api" / "add" / NanoboardMessage.HASH_FORMAT) & entity(as[String])) { (parent, message) ⇒
        queue.offer(NanoboardMessage(parent, BitMessageTransport.fromBase64(message)))
        complete(StatusCodes.OK)
      }
    }
  }
}
