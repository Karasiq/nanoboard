package com.karasiq.nanoboard

import scala.util.Try

import akka.util.ByteString

import com.karasiq.nanoboard.encoding.NanoboardCrypto.{sha256, BCDigestOps}
import com.karasiq.nanoboard.encoding.formats.{CBORMessagePackFormat, MessagePackFormat, TextMessagePackFormat}
import com.karasiq.nanoboard.utils.{ByteStringOps, _}

case class NanoboardMessage(parent: String, text: String, pow: ByteString = NanoboardMessage.NoPOW, signature: ByteString = NanoboardMessage.NoSignature) {
  val hash: String = sha256.digest(ByteString(parent + NanoboardMessage.textWithSignatureTags(this))).take(16).toHexString()
}

object NanoboardMessage extends MessagePackFormat {
  // Constants
  val HashLength = 32
  val HashFormat = "(?i)[a-f0-9]{32}".r
  val POWLength = 128
  val SignatureLength = 64

  val NoPOW = ByteString(Array.fill[Byte](POWLength)(0))
  val NoSignature = ByteString(Array.fill[Byte](SignatureLength)(0))

  //noinspection ScalaDeprecation
  override def parseMessages(payload: ByteString): Vector[NanoboardMessage] = {
    Try(CBORMessagePackFormat.parseMessages(payload))
      .orElse(Try(TextMessagePackFormat.parseMessages(payload)))
      .get
  }

  override def writeMessages(messages: Seq[NanoboardMessage]): ByteString = {
    TextMessagePackFormat.writeMessages(messages) // CBORMessagePackFormat.writeMessages(messages)
  }

  private[nanoboard] def getPOWTag(pow: ByteString) = {
    if (pow.isEmpty || pow == NoPOW) "" else s"[pow=${pow.toHexString()}]"
  }

  private[nanoboard] def getSignatureTag(signature: ByteString) = {
    if (signature.isEmpty || signature == NoSignature) "" else s"[sign=${signature.toHexString()}]"
  }

  private[nanoboard] def stripSignatureTags(message: String): (String, Option[ByteString], Option[ByteString]) = {
    val regex = "(?i)\\[pow=([0-9a-f]+)\\]\\[sign=([0-9a-f]{128})\\]".r
    regex.findFirstMatchIn(message) match {
      case Some(m @ regex(pow, sign)) ⇒
        (message.take(m.start), Some(ByteString.fromHexString(pow)), Some(ByteString.fromHexString(sign)))

      case _ ⇒
        (message, None, None)
    }
  }

  private[nanoboard] def textWithSignatureTags(m: NanoboardMessage): String = {
    m.text + getPOWTag(m.pow) + getSignatureTag(m.signature)
  }
}
