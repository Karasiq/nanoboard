package com.karasiq.nanoboard.encoding.stages

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataCipher.{BCDigestOps, BCStreamCipherOps, sha256}
import com.karasiq.nanoboard.encoding.DataEncodingStage
import com.typesafe.config.{Config, ConfigFactory}
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}

object SalsaCipher {
  def apply(key: String): SalsaCipher = {
    new SalsaCipher(key)
  }

  def apply(config: Config): SalsaCipher = {
    apply(config.getString("nanoboard.encryption-key"))
  }

  def apply(): SalsaCipher = {
    apply(ConfigFactory.load())
  }
}

final class SalsaCipher(key: String) extends DataEncodingStage {
  private def createCipher(encryption: Boolean): Salsa20Engine = {
    val cipher = new Salsa20Engine()

    val keyBytes = ByteString(key)
    val secretKey = new KeyParameter(sha256.digest(keyBytes).toArray, 0, 32)
    val iv: Array[Byte] = sha256.digest(keyBytes.reverse).toArray
    cipher.init(encryption, new ParametersWithIV(secretKey, iv, 0, 8))
    cipher
  }

  override def encode(data: ByteString): ByteString = {
    createCipher(encryption = true).process(data)
  }

  override def decode(data: ByteString): ByteString = {
    createCipher(encryption = false).process(data)
  }
}
