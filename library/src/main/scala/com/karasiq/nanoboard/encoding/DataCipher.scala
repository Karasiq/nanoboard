package com.karasiq.nanoboard.encoding

import java.security.MessageDigest

import org.bouncycastle.jce.provider.BouncyCastleProvider

private[encoding] object DataCipher {
  val provider = new BouncyCastleProvider

  @inline
  def sha256 = MessageDigest.getInstance("SHA256", provider)
}
