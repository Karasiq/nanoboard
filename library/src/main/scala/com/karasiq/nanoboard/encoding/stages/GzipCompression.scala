package com.karasiq.nanoboard.encoding.stages

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage

/**
  * GZIP compression data stage.
  */
object GzipCompression extends GzipCompression {
  def apply(): GzipCompression = this
}

sealed class GzipCompression extends DataEncodingStage {
  /**
    * Compresses data with GZIP
    * @param data Source data
    * @return Compressed data
    */
  override def encode(data: ByteString): ByteString = {
    val inputStream = new ByteArrayInputStream(data.toArray)
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val outputStream = new GZIPOutputStream(byteArrayOutputStream, 1024, true)
    try {
      val buffer = new Array[Byte](1024)
      var len = 0
      while (len != -1) {
        len = inputStream.read(buffer)
        if (len > 0) outputStream.write(buffer, 0, len)
      }
      outputStream.finish()
      outputStream.flush()
      ByteString(byteArrayOutputStream.toByteArray)
    } finally {
      inputStream.close()
      outputStream.close()
    }
  }

  /**
    * Decompresses GZIP-compressed data
    * @param data Previously compressed data
    * @return Source data
    */
  override def decode(data: ByteString): ByteString = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(data.toArray), 1024)
    val outputStream = new ByteArrayOutputStream()
    try {
      val buffer = new Array[Byte](1024)
      var len = 0
      while (len != -1) {
        len = inputStream.read(buffer)
        if (len > 0) outputStream.write(buffer, 0, len)
      }
      ByteString(outputStream.toByteArray)
    } finally {
      inputStream.close()
      outputStream.close()
    }
  }
}
