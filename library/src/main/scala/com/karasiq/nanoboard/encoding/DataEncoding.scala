package com.karasiq.nanoboard.encoding

import java.io.{InputStream, OutputStream}

import akka.util.ByteString

trait DataEncoding {
  def encode(input: InputStream, data: ByteString, output: OutputStream): Unit
  def decode(input: InputStream): ByteString
}
