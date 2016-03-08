package com.karasiq.nanoboard.server

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import boopickle.Default._

private[server] trait BinaryMarshaller {
  implicit def defaultMarshaller[T: Pickler]: ToEntityMarshaller[T] = {
    val contentType = ContentTypes.`application/octet-stream`
    Marshaller.withFixedContentType(contentType)((value: T) ⇒ HttpEntity(contentType, ByteString(Pickle.intoBytes(value))))
  }

  implicit def defaultUnmarshaller[A, B](implicit ev: Pickler[B], m: Unmarshaller[A, ByteString]): Unmarshaller[A, B] = {
    m.map(bs ⇒ Unpickle[B].fromBytes(bs.toByteBuffer))
  }
}
