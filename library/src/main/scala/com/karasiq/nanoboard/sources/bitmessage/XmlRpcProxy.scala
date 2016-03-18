package com.karasiq.nanoboard.sources.bitmessage

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.stream.ActorMaterializer
import com.karasiq.nanoboard.sources.bitmessage.XmlRpcProxy.{XmlRpcParameter, XmlRpcTags}

import scala.concurrent.Future
import scala.language.{dynamics, implicitConversions}
import scalatags.Text.all._
import scalatags.text.Builder

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
    http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://$apiAddress:$apiPort/", entity = HttpEntity(ContentTypes.`text/xml(UTF-8)`, entity), headers = List(authentication)))
  }
}

private[bitmessage] object XmlRpcProxy {
  object XmlRpcTags {
    val methodCall = "methodCall".tag
    val methodName = "methodName".tag
    val params = "params".tag
    val param = "param".tag
    val value = "value".tag
    val int = "int".tag
  }

  sealed trait XmlDataWrapper[T] extends (T ⇒ Modifier)

  implicit object StringXmlDataWrapper extends XmlDataWrapper[String] {
    def apply(v1: String) = v1
  }

  implicit object IntXmlDataWrapper extends XmlDataWrapper[Int] {
    def apply(v1: Int) = XmlRpcTags.int(v1)
  }

  implicit object UnitXmlDataWrapper extends XmlDataWrapper[Unit] {
    def apply(v1: Unit) = ()
  }

  sealed trait XmlRpcParameter extends Modifier

  implicit def anyToXmlRpcParameter[T: XmlDataWrapper](value: T): XmlRpcParameter = new XmlRpcParameter {
    def applyTo(t: Builder) = value.applyTo(t)
  }
}
