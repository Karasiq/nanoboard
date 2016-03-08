package com.karasiq.nanoboard.server

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import upickle.default._

private[server] trait JsonMarshaller {
  implicit def defaultMarshaller[T: Writer]: ToEntityMarshaller[T] = {
    Marshaller.withFixedContentType(ContentTypes.`application/json`)((value: T) ⇒ HttpEntity(ContentTypes.`application/json`, write(value)))
  }

  implicit def defaultUnmarshaller[A, B](implicit ev: Reader[B], m: Unmarshaller[A, String]): Unmarshaller[A, B] = {
    m.map(read[B])
  }
}
