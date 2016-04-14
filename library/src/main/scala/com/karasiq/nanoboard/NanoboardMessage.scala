package com.karasiq.nanoboard

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.NanoboardCrypto.{BCDigestOps, sha256}
import com.karasiq.nanoboard.encoding.formats.{CBORMessagePackFormat, MessagePackFormat, TextMessagePackFormat}
import com.karasiq.nanoboard.utils.ByteStringOps

import scala.util.Try

case class NanoboardMessage(parent: String, text: String, pow: ByteString = NanoboardMessage.NO_POW, signature: ByteString = NanoboardMessage.NO_SIGNATURE) {
  val hash: String = sha256.digest(ByteString(parent + text)).take(16).toHexString()
}

object NanoboardMessage extends MessagePackFormat {
  // Constants
  val HASH_LENGTH = 32
  val HASH_FORMAT = "(?i)[a-f0-9]{32}".r
  val POW_LENGTH = 128
  val SIGNATURE_LENGTH = 64

  val NO_POW = ByteString(Array.fill[Byte](POW_LENGTH)(0))
  val NO_SIGNATURE = ByteString(Array.fill[Byte](SIGNATURE_LENGTH)(0))

  //noinspection ScalaDeprecation
  override def parseMessages(payload: ByteString): Vector[NanoboardMessage] = {
    Try(CBORMessagePackFormat.parseMessages(payload))
      .orElse(Try(TextMessagePackFormat.parseMessages(payload)))
      .get
  }

  override def writeMessages(messages: Seq[NanoboardMessage]): ByteString = {
    CBORMessagePackFormat.writeMessages(messages)
  }
}
