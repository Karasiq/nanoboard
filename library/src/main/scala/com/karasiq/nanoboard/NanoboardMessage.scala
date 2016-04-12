package com.karasiq.nanoboard

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DefaultNanoboardMessageFormat
import com.karasiq.nanoboard.encoding.NanoboardCrypto.{BCDigestOps, sha256}
import org.apache.commons.codec.binary.Hex

case class NanoboardMessage(parent: String, text: String) {
  def payload: String = parent + text
  val hash: String = Hex.encodeHexString(sha256.digest(ByteString(payload)).toArray).take(32)
}

object NanoboardMessage extends DefaultNanoboardMessageFormat {
  val HASH_LENGTH = 32
  val HASH_FORMAT = "(?i)[a-f0-9]{32}".r
}
