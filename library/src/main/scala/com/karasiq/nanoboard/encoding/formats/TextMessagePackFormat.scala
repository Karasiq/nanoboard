package com.karasiq.nanoboard.encoding.formats

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.utils._

import scala.annotation.tailrec

/**
  * Legacy nanoboard message pack format.
  * Output starts with six-digits hex numbers, the first of which is a count of messages,
  * and subsequent are lengths of the messages itself, and followed by concatenated messages in format `replyTo` + `text`.
  * @see [[https://github.com/nanoboard/nanoboard/blob/master/Database/NanoPostPackUtil.cs Original implementation]]
  */
trait TextMessagePackFormat extends MessagePackFormat {
  final def parseMessages(payloadBs: ByteString): Vector[NanoboardMessage] = {
    val payload = payloadBs.utf8String
    val sizes: Vector[Int] = {
      val sizes: Iterator[Int] = payload.grouped(6)
        .map(bs ⇒ Integer.parseInt(bs, 16))

      val count = if (sizes.nonEmpty) sizes.next() else 0
      sizes.take(count).toVector
    }

    @tailrec
    def parse(str: String, sizes: Vector[Int], messages: Vector[NanoboardMessage] = Vector.empty): Vector[NanoboardMessage] = {
      if (sizes.isEmpty) {
        messages
      } else {
        val (data, rest) = str.splitAt(sizes.head)
        val (hash, rawMessage) = data.splitAt(NanoboardMessage.HASH_LENGTH)
        val (message, pow, signature) = TextMessagePackFormat.stripSignatureTags(rawMessage)
        parse(rest, sizes.tail, messages :+ NanoboardMessage(hash, message, pow.getOrElse(NanoboardMessage.NO_POW), signature.getOrElse(NanoboardMessage.NO_SIGNATURE)))
      }
    }

    val data = payload.drop((sizes.length + 1) * 6)
    assert(sizes.forall(_ > NanoboardMessage.HASH_LENGTH) && sizes.sum <= data.length, "Invalid message sizes")
    parse(data, sizes)
  }

  @deprecated("Use CBOR format", "1.2.0")
  final def writeMessages(messages: Seq[NanoboardMessage]): ByteString = {
    val payloads = messages.map(m ⇒ m.parent + TextMessagePackFormat.textWithSignatureTags(m))
    val sizes = Vector(payloads.length) ++ payloads.map(_.length)
    ByteString((sizes.map(size ⇒ f"$size%06x") ++ payloads).mkString)
  }
}

object TextMessagePackFormat extends TextMessagePackFormat {
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
    m.text + s"[pow=${m.pow.toHexString()}][sign=${m.signature.toHexString()}]"
  }
}
