package com.karasiq.nanoboard

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DefaultNanoboardMessageFormat
import com.karasiq.nanoboard.encoding.NanoboardCrypto.{BCDigestOps, sha256}
import com.karasiq.nanoboard.utils.ByteStringOps

case class NanoboardMessage(parent: String, text: String) {
  def payload: String = parent + text
  val hash: String = sha256.digest(ByteString(payload)).toHexString().take(32)
}

object NanoboardMessage extends DefaultNanoboardMessageFormat {
  val HASH_LENGTH = 32
  val HASH_FORMAT = "(?i)[a-f0-9]{32}".r
}
