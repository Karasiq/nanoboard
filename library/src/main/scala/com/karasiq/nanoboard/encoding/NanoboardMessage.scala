package com.karasiq.nanoboard.encoding

import org.apache.commons.codec.binary.Hex

import scala.annotation.tailrec

case class NanoboardMessage(answerTo: String, text: String) {
  def payload: String = answerTo + text
  def hash: String = Hex.encodeHexString(DataCipher.sha256.digest(payload.getBytes("UTF-8")))
}

object NanoboardMessage {
  def parseMessages(payload: String): Vector[NanoboardMessage] = {
    val sizes = {
      val sizes = payload.grouped(6)
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
        val (hash, message) = data.splitAt(32)
        parse(rest, sizes.tail, messages :+ NanoboardMessage(hash, message))
      }
    }

    parse(payload.drop((sizes.length + 1) * 6), sizes)
  }

  def writeMessages(messages: Seq[NanoboardMessage]): String = {
    val payloads = messages.map(_.payload)
    val sizes = Vector(payloads.length) ++ payloads.map(_.length)
    (sizes.map(size ⇒ f"$size%06x") ++ payloads).mkString
  }
}
