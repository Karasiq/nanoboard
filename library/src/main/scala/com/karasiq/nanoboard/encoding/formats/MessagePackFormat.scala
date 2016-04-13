package com.karasiq.nanoboard.encoding.formats

import akka.util.ByteString
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
    * Serializes messages to string
    * @param messages Messages
    * @return Serialized messages
    */
  def writeMessages(messages: Seq[NanoboardMessage]): ByteString
}
