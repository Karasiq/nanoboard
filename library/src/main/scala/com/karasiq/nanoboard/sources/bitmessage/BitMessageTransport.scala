package com.karasiq.nanoboard.sources.bitmessage

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.karasiq.nanoboard.NanoboardMessage
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.Charsets
import org.apache.commons.codec.binary.Base64
import upickle.default._

import scala.concurrent.Future
import scalatags.Text.all._

object BitMessageTransport {
  def apply(config: Config = ConfigFactory.load())(implicit ac: ActorSystem, am: ActorMaterializer) = {
    new BitMessageTransport(config)
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

final class BitMessageTransport(config: Config)(implicit ac: ActorSystem, am: ActorMaterializer) {
  // Settings
  private val apiAddress = config.getString("nanoboard.bitmessage.host")
  private val apiPort = config.getString("nanoboard.bitmessage.port")
  private val apiUsername = config.getString("nanoboard.bitmessage.username")
  private val apiPassword = config.getString("nanoboard.bitmessage.password")
  private val chanAddress = config.getString("nanoboard.bitmessage.chan-address")

  // Input/output
  def sendMessage(message: NanoboardMessage): Future[HttpResponse] = {
    import XmlRpcTags._
    val entity = "<?xml version=\"1.0\"?>" + methodCall(
      methodName("sendMessage"),
      params(
        param(value(chanAddress)),
        param(value(chanAddress)),
        param(value()),
        param(value(BitMessageTransport.asBase64(BitMessageTransport.wrap(message)))),
        param(value(int(2))),
        param(value(int(21600)))
      )
    )
    val authentication = Authorization(BasicHttpCredentials(apiUsername, apiPassword))
    http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://$apiAddress:$apiPort/", entity = HttpEntity(ContentTypes.`text/xml(UTF-8)`, entity), headers = List(authentication)))
  }

  def receiveMessages(host: String, port: Int, sink: Sink[NanoboardMessage, _]): Future[Http.ServerBinding] = {
    http.bindAndHandle(route(sink), host, port)
  }

  // Handlers
  private val http = Http()

  private def route(sink: Sink[NanoboardMessage, _]) = {
    val queue = Source
      .queue(20, OverflowStrategy.dropHead)
      .to(sink)
      .run()

    post {
      (path("api" / "add" / NanoboardMessage.hashRegex) & entity(as[String])) { (parent, message) ⇒
        queue.offer(NanoboardMessage(parent, BitMessageTransport.fromBase64(message)))
        complete(StatusCodes.OK)
      }
    }
  }
}
