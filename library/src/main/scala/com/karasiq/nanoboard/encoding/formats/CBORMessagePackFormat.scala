package com.karasiq.nanoboard.encoding.formats

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.upokecenter.cbor.CBORObject

import scala.collection.JavaConversions._

/**
  * CBOR message pack format
  * @see [[http://cbor.io Concise Binary Object Representation]]
  */
trait CBORMessagePackFormat extends MessagePackFormat {
  /**
    * Parses messages from serialized data
    * @param payload Serialized messages
    * @return Parsed messages
    */
  override def parseMessages(payload: ByteString): Vector[NanoboardMessage] = {
    val messages = CBORObject.DecodeFromBytes(payload.toArray).get("messages")
    messages.getValues.toVector.map { message ⇒
      NanoboardMessage(message.get("parent").AsString(), message.get("text").AsString(), ByteString(message.get("pow").GetByteString()), ByteString(message.get("sign").GetByteString()))
    }
  }

  /**
    * Serializes messages to byte string
    * @param messages Messages
    * @return Serialized messages
    */
  override def writeMessages(messages: Seq[NanoboardMessage]): ByteString = {
    val messagesArray = messages.foldLeft(CBORObject.NewArray()) {
      case (array, NanoboardMessage(parent, text, pow, signature)) ⇒
        array.Add(
          CBORObject.NewMap()
            .Add("parent", parent)
            .Add("text", text)
            .Add("pow", pow.toArray[Byte])
            .Add("sign", signature.toArray[Byte])
        )
    }
    val payload = CBORObject.NewMap().Add("messages", messagesArray)
    ByteString(payload.EncodeToBytes())
  }
}

object CBORMessagePackFormat extends CBORMessagePackFormat