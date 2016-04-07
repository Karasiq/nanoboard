package com.karasiq.nanoboard.encoding

import com.karasiq.nanoboard.NanoboardMessage

import scala.annotation.tailrec

/**
  * Nanoboard message pack format
  */
trait NanoboardMessageFormat {
  /**
    * Parses messages from serialized data
    * @param payload Serialized messages
    * @return Parsed messages
    */
  def parseMessages(payload: String): Vector[NanoboardMessage]

  /**
    * Serializes messages to string
    * @param messages Messages
    * @return Serialized messages
    */
  def writeMessages(messages: Seq[NanoboardMessage]): String
}

/**
  * Default nanoboard message pack format.
  * Output starts with six-digits hex numbers, the first of which is a count of messages,
  * and subsequent are lengths of the messages itself, and followed by concatenated messages in format `replyTo` + `text`.
  * @see [[https://github.com/nanoboard/nanoboard/blob/master/Database/NanoPostPackUtil.cs Original implementation]]
  */
trait DefaultNanoboardMessageFormat extends NanoboardMessageFormat {
  final def parseMessages(payload: String): Vector[NanoboardMessage] = {
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
        val (hash, message) = data.splitAt(32)
        parse(rest, sizes.tail, messages :+ NanoboardMessage(hash, message))
      }
    }

    val data = payload.drop((sizes.length + 1) * 6)
    assert(sizes.forall(_ > 32) && sizes.sum <= data.length, "Invalid message sizes")
    parse(data, sizes)
  }

  final def writeMessages(messages: Seq[NanoboardMessage]): String = {
    val payloads = messages.map(_.payload)
    val sizes = Vector(payloads.length) ++ payloads.map(_.length)
    (sizes.map(size ⇒ f"$size%06x") ++ payloads).mkString
  }
}
