package com.karasiq.nanoboard.encoding

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.security.MessageDigest
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import akka.util.ByteString
import org.apache.commons.codec.Charsets
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}
import org.bouncycastle.jce.provider.BouncyCastleProvider

private[encoding] object DataCipher {
  val provider = new BouncyCastleProvider

  @inline
  def sha256 = MessageDigest.getInstance("SHA256", provider)
}

trait DataEncodingStage {
  def encode(data: ByteString): ByteString
  def decode(data: ByteString): ByteString
}

final class SalsaCipher(key: String) extends DataEncodingStage {
  private def createCipher(encryption: Boolean): Salsa20Engine = {
    val cipher = new Salsa20Engine()

    val secretKey: KeyParameter = new KeyParameter(DataCipher.sha256.digest(key.getBytes(Charsets.UTF_8)), 0, 32)
    val iv: Array[Byte] = DataCipher.sha256.digest(key.getBytes("UTF-8").reverse)
    cipher.init(encryption, new ParametersWithIV(secretKey, iv, 0, 8))
    cipher
  }

  override def encode(data: ByteString): ByteString = {
    val buffer = new Array[Byte](data.length)
    createCipher(encryption = true).processBytes(data.toArray, 0, data.length, buffer, 0)
    ByteString(buffer)
  }

  override def decode(data: ByteString): ByteString = {
    val buffer = new Array[Byte](data.length)
    createCipher(encryption = false).processBytes(data.toArray, 0, data.length, buffer, 0)
    ByteString(buffer)
  }
}

object GzipCompression extends DataEncodingStage {
  override def encode(data: ByteString): ByteString = {
    val inputStream = new ByteArrayInputStream(data.toArray)
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val outputStream = new GZIPOutputStream(byteArrayOutputStream)
    val buffer = new Array[Byte](1024)
    var len = 1
    while (len > 0) {
      len = inputStream.read(buffer)
      if (len > 0) outputStream.write(buffer, 0, len)
    }
    outputStream.flush()
    val result = ByteString(byteArrayOutputStream.toByteArray)
    inputStream.close()
    outputStream.close()
    result
  }

  override def decode(data: ByteString): ByteString = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(data.toArray))
    val outputStream = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)
    var len = 1
    while (len > 0) {
      len = inputStream.read(buffer)
      if (len > 0) outputStream.write(buffer, 0, len)
    }
    val result = ByteString(outputStream.toByteArray)
    inputStream.close()
    outputStream.close()
    result
  }
}