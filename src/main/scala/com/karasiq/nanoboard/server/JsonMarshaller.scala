package com.karasiq.nanoboard.server

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import play.api.libs.json._

private[server] trait JsonMarshaller {
  implicit def defaultMarshaller[T: Writes]: ToEntityMarshaller[T] = {
    Marshaller.withFixedContentType(ContentTypes.`application/json`)((value: T) ⇒ HttpEntity(ContentTypes.`application/json`, Json.toJson(value).toString()))
  }

  def defaultUnmarshaller[A, B](implicit ev: Reads[B], m: Unmarshaller[A, String]): Unmarshaller[A, B] = {
    m.map(str ⇒ Json.parse(str).as[B])
  }
}
