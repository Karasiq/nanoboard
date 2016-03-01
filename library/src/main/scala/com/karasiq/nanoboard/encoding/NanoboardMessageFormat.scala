package com.karasiq.nanoboard.encoding

import scala.annotation.tailrec

trait NanoboardMessageFormat {
  def parseMessages(payload: String): Vector[NanoboardMessage]

  def writeMessages(messages: Seq[NanoboardMessage]): String
}

trait DefaultNanoboardMessageFormat extends NanoboardMessageFormat {
  final def parseMessages(payload: String): Vector[NanoboardMessage] = {
    val sizes = {
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

    parse(payload.drop((sizes.length + 1) * 6), sizes)
  }

  final def writeMessages(messages: Seq[NanoboardMessage]): String = {
    val payloads = messages.map(_.payload)
    val sizes = Vector(payloads.length) ++ payloads.map(_.length)
    (sizes.map(size ⇒ f"$size%06x") ++ payloads).mkString
  }
}
