package com.karasiq.nanoboard.encoding.stages

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage

object GzipCompression extends GzipCompression {
  def apply(): GzipCompression = this
}

sealed class GzipCompression extends DataEncodingStage {
  override def encode(data: ByteString): ByteString = {
    val inputStream = new ByteArrayInputStream(data.toArray)
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val outputStream = new GZIPOutputStream(byteArrayOutputStream, 1024, true)
    val buffer = new Array[Byte](1024)
    var len = 0
    while (len != -1) {
      len = inputStream.read(buffer)
      if (len > 0) outputStream.write(buffer, 0, len)
    }
    outputStream.finish()
    outputStream.flush()
    val result = ByteString(byteArrayOutputStream.toByteArray)
    inputStream.close()
    outputStream.close()
    result
  }

  override def decode(data: ByteString): ByteString = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(data.toArray), 1024)
    val outputStream = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)
    var len = 0
    while (len != -1) {
      len = inputStream.read(buffer)
      if (len > 0) outputStream.write(buffer, 0, len)
    }
    val result = ByteString(outputStream.toByteArray)
    inputStream.close()
    outputStream.close()
    result
  }
}
