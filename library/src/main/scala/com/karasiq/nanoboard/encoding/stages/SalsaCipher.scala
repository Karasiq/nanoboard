package com.karasiq.nanoboard.encoding.stages

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.{DataCipher, DataEncodingStage}
import org.apache.commons.codec.Charsets
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}

object SalsaCipher {
  def apply(key: String): SalsaCipher = {
    new SalsaCipher(key)
  }
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
    val length = createCipher(encryption = true).processBytes(data.toArray, 0, data.length, buffer, 0)
    ByteString(buffer).take(length)
  }

  override def decode(data: ByteString): ByteString = {
    val buffer = new Array[Byte](data.length)
    val length = createCipher(encryption = false).processBytes(data.toArray, 0, data.length, buffer, 0)
    ByteString(buffer).take(length)
  }
}
