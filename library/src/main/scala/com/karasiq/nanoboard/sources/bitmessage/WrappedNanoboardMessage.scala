package com.karasiq.nanoboard.sources.bitmessage

private[bitmessage] case class WrappedNanoboardMessage(hash: String, message: String, replyTo: String) {
  assert(hash.length == 32 && replyTo.length == 32, "Invalid hashes")
}
