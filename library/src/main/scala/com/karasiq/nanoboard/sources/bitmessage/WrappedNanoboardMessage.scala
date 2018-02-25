package com.karasiq.nanoboard.sources.bitmessage

import com.karasiq.nanoboard.NanoboardMessage

private[bitmessage] case class WrappedNanoboardMessage(hash: String, message: String, replyTo: String) {
  assert(hash.length == NanoboardMessage.HashLength && replyTo.length == NanoboardMessage.HashLength, "Invalid hashes")
}
