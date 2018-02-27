package com.karasiq.nanoboard.sources.bitmessage

import scala.concurrent.Future
import scala.language.{dynamics, implicitConversions}

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.stream.ActorMaterializer
import scalatags.Text.all._
import scalatags.text.Builder

import com.karasiq.nanoboard.sources.bitmessage.XmlRpcProxy.{XmlRpcParameter, XmlRpcTags}

/**
  * Simple XML-RPC wrapper, based on `akka-http`
  */
private[bitmessage] final class XmlRpcProxy(http: HttpExt, apiAddress: String, apiPort: Int, apiUsername: String, apiPassword: String)(implicit am: ActorMaterializer) extends Dynamic {
  def applyDynamic(method: String)(args: XmlRpcParameter*): Future[HttpResponse] = {
    import XmlRpcTags._
    val entity = "<?xml version=\"1.0\"?>" + methodCall(
      methodName(method),
      params(
        for (arg ← args) yield param(value(arg))
      )
    )
    val authentication = Authorization(BasicHttpCredentials(apiUsername, apiPassword))
    val url = s"http://$apiAddress:$apiPort/"
    http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = url, entity = HttpEntity(ContentTypes.`text/xml(UTF-8)`, entity), headers = List(authentication)))
  }
}

private[bitmessage] object XmlRpcProxy {
  object XmlRpcTags {
    val methodCall = tag("methodCall")
    val methodName = tag("methodName")
    val params = tag("params")
    val param = tag("param")
    val value = tag("value")
    val int = tag("int")
  }

  sealed trait XmlDataWrapper[T] {
    def toModifier(value: T): Modifier
  }

  implicit object StringXmlDataWrapper extends XmlDataWrapper[String] {
    def toModifier(value: String) = value
  }

  implicit object IntXmlDataWrapper extends XmlDataWrapper[Int] {
    def toModifier(value: Int) = XmlRpcTags.int(value)
  }

  implicit object UnitXmlDataWrapper extends XmlDataWrapper[Unit] {
    def toModifier(value: Unit) = ()
  }

  sealed trait XmlRpcParameter extends Modifier

  implicit def anyToXmlRpcParameter[T: XmlDataWrapper](value: T): XmlRpcParameter = new XmlRpcParameter {
    def applyTo(t: Builder) = implicitly[XmlDataWrapper[T]].toModifier(value).applyTo(t)
  }
}
