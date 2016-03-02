package com.karasiq.nanoboard.server

import akka.http.scaladsl.marshalling.Marshaller
import upickle.default._

trait UpickleMarshaller {
  implicit def jsonMarshaller[A, B](implicit ev: Writer[A], m: Marshaller[String, B]): Marshaller[A, B] = {
    Marshaller(implicit ec ⇒ value ⇒ m(write(value)))
  }
}
