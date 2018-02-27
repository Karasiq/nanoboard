package com.karasiq.nanoboard.frontend.api

import java.nio.ByteBuffer

import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

import boopickle.Default._

private[api] object BinaryMarshaller {
  def responseType: String = "arraybuffer"

  def write[T: Pickler](value: T): ByteBuffer = {
    Pickle.intoBytes(value)
  }

  def read[T: Pickler](value: Any): T = {
    Unpickle[T].fromBytes(TypedArrayBuffer.wrap(value.asInstanceOf[ArrayBuffer]))
  }
}
