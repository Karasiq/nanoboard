package com.karasiq.nanoboard.sources.bitmessage

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.karasiq.nanoboard.NanoboardMessage
import com.typesafe.config.Config
import org.apache.commons.codec.Charsets
import org.apache.commons.codec.binary.Base64
import upickle.default._

import scala.concurrent.Future
import scalatags.Text.all._

private[bitmessage] case class WrappedNanoboardMessage(hash: String, message: String, replyTo: String) {
  assert(hash.length == 32 && replyTo.length == 32, "Invalid hashes")
}

private[bitmessage] object XmlRpcTags {
  val methodCall = "methodCall".tag
  val methodName = "methodName".tag
  val params = "params".tag
  val param = "param".tag
  val value = "value".tag
  val int = "int".tag
}

final class BitMessageTransport(config: Config, f: NanoboardMessage ⇒ Unit)(implicit ac: ActorSystem, am: ActorMaterializer) {
  private val http = Http()
  private val apiAddress = config.getString("nanoboard.bitmessage.host")
  private val apiPort = config.getString("nanoboard.bitmessage.port")
  private val apiUsername = config.getString("nanoboard.bitmessage.username")
  private val apiPassword = config.getString("nanoboard.bitmessage.password")
  private val chanAddress = config.getString("nanoboard.bitmessage.chan-address")
  private val sha256HashRegex = "[A-Za-z0-9]{32}".r

  @inline
  private def asBase64(string: String): String = {
    Base64.encodeBase64String(string.getBytes(Charsets.UTF_8))
  }

  @inline
  private def fromBase64(string: String): String = {
    new String(Base64.decodeBase64(string), Charsets.UTF_8)
  }

  def sendMessage(message: NanoboardMessage): Future[HttpResponse] = {
    val wrapped = WrappedNanoboardMessage(message.hash, asBase64(message.text), message.parent)
    import XmlRpcTags._
    val entity = "<?xml version=\"1.0\"?>" + methodCall(
      methodName("sendMessage"),
      params(
        param(value(chanAddress)),
        param(value(chanAddress)),
        param(value()),
        param(value(asBase64(write(Some(wrapped))))),
        param(value(int(2))),
        param(value(int(21600)))
      )
    )
    val authentication = Authorization(BasicHttpCredentials(apiUsername, apiPassword))
    http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://$apiAddress:$apiPort/", entity = HttpEntity(ContentTypes.`text/xml(UTF-8)`, entity), headers = List(authentication)))
  }

  val route = {
    post {
      (path("api" / "add" / sha256HashRegex) & entity(as[String])) { (parent, message) ⇒
        f(NanoboardMessage(parent, fromBase64(message)))
        complete(StatusCodes.OK)
      }
    }
  }
}
