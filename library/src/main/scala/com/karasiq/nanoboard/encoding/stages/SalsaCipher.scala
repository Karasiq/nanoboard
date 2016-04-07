package com.karasiq.nanoboard.encoding.stages

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage
import com.karasiq.nanoboard.encoding.NanoboardCrypto.{BCDigestOps, BCStreamCipherOps, sha256}
import com.typesafe.config.Config
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}

object SalsaCipher {
  /**
    * Creates Salsa20 cipher stage from specified config
    * @param nbConfig Configuration object
    * @return Salsa20 cipher stage
    */
  def fromConfig(nbConfig: Config): SalsaCipher = {
    apply(nbConfig.getString("encryption-key"))
  }

  /**
    * Creates Salsa20 cipher stage with the specified key
    * @param key Encryption key
    * @return Salsa20 cipher stage
    */
  def apply(key: String): SalsaCipher = {
    new SalsaCipher(key)
  }
}

/**
  * Salsa20 stream cipher encryption stage
  * @param key Encryption key
  */
final class SalsaCipher(key: String) extends DataEncodingStage {
  private def createCipher(encryption: Boolean): Salsa20Engine = {
    val cipher = new Salsa20Engine()

    val keyBytes = ByteString(key)
    val secretKey = new KeyParameter(sha256.digest(keyBytes).toArray, 0, 32)
    val iv: Array[Byte] = sha256.digest(keyBytes.reverse).toArray
    cipher.init(encryption, new ParametersWithIV(secretKey, iv, 0, 8))
    cipher
  }

  /**
    * Encrypts provided data with the Salsa20 stream cipher
    * @param data Source data
    * @return Encrypted data
    */
  override def encode(data: ByteString): ByteString = {
    createCipher(encryption = true).process(data)
  }

  /**
    * Decrypts data, encrypted with the Salsa20 stream cipher
    * @param data Encrypted data
    * @return Source data
    */
  override def decode(data: ByteString): ByteString = {
    createCipher(encryption = false).process(data)
  }
}
