package com.karasiq.nanoboard

import com.karasiq.nanoboard.encoding.{DataCipher, DefaultNanoboardMessageFormat}
import org.apache.commons.codec.binary.Hex

case class NanoboardMessage(parent: String, text: String) {
  def payload: String = parent + text
  def hash: String = Hex.encodeHexString(DataCipher.sha256.digest(payload.getBytes("UTF-8"))).take(32)
}

object NanoboardMessage extends DefaultNanoboardMessageFormat {
  val hashFormat = "(?i)[a-f0-9]{32}".r
}
