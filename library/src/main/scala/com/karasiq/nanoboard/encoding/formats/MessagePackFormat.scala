package com.karasiq.nanoboard.encoding.formats

import akka.util.ByteString
import com.typesafe.config.Config

import com.karasiq.nanoboard.NanoboardMessage

/**
  * Nanoboard message pack format
  */
trait MessagePackFormat {
  /**
    * Parses messages from serialized data
    * @param payload Serialized messages
    * @return Parsed messages
    */
  def parseMessages(payload: ByteString): Vector[NanoboardMessage]

  /**
    * Serializes messages to byte string
    * @param messages Messages
    * @return Serialized messages
    */
  def writeMessages(messages: Seq[NanoboardMessage]): ByteString
}

object MessagePackFormat {
  def apply(config: Config): MessagePackFormat = config.getString("nanoboard.message-pack-format").toLowerCase match {
    case "text" ⇒ TextMessagePackFormat
    case "cbor" ⇒ CBORMessagePackFormat
    case format ⇒ throw new IllegalArgumentException(s"Invalid format: $format")
  }
}