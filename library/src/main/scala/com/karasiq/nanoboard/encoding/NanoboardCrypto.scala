package com.karasiq.nanoboard.encoding

import akka.util.ByteString
import org.bouncycastle.crypto.digests.{SHA256Digest, SHA512Digest}
import org.bouncycastle.crypto.{Digest, StreamCipher}
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * Internal cryptography utility
  */
private[nanoboard] object NanoboardCrypto {
  val provider = new BouncyCastleProvider

  @inline
  def sha256 = new SHA256Digest()

  @inline
  def sha512 = new SHA512Digest()

  implicit class BCDigestOps[T <: Digest](md: T) {
    def digest(data: ByteString): ByteString = {
      md.update(data.toArray, 0, data.length)
      val hash = Array.ofDim[Byte](md.getDigestSize)
      md.doFinal(hash, 0)
      ByteString(hash)
    }

    def updated(data: ByteString): T = {
      md.update(data.toArray, 0, data.length)
      md
    }
  }

  implicit class BCStreamCipherOps[T <: StreamCipher](cipher: T) {
    def process(data: ByteString): ByteString = {
      val buffer = Array.ofDim[Byte](data.length)
      val length = cipher.processBytes(data.toArray, 0, data.length, buffer, 0)
      ByteString(buffer).take(length)
    }
  }
}
