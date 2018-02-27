package com.karasiq.nanoboard.sources.bitmessage

import play.api.libs.json.Json

import com.karasiq.nanoboard.NanoboardMessage

@SerialVersionUID(0L)
private[bitmessage] final case class WrappedNanoboardMessage(hash: String, message: String, replyTo: String) {
  assert(hash.length == NanoboardMessage.HashLength && replyTo.length == NanoboardMessage.HashLength, "Invalid hashes")
}

private[bitmessage] object WrappedNanoboardMessage {
  implicit val wrappedNanoboardMessageFormat = Json.format[WrappedNanoboardMessage]
}
