package com.karasiq.nanoboard

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataCipher.{BCDigestOps, sha256}
import com.karasiq.nanoboard.encoding.{DataCipher, DefaultNanoboardMessageFormat}
import org.apache.commons.codec.binary.Hex

case class NanoboardMessage(parent: String, text: String) {
  def payload: String = parent + text
  def hash: String = Hex.encodeHexString(sha256.digest(ByteString(payload)).toArray).take(32)
}

object NanoboardMessage extends DefaultNanoboardMessageFormat {
  val HASH_FORMAT = "(?i)[a-f0-9]{32}".r
}
