package com.karasiq.nanoboard.model

import java.time.Instant

import akka.util.ByteString
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.language.implicitConversions

private[nanoboard] object MessageConversions {
  def wrapMessage(message: NanoboardMessage, container: Option[Long] = None, answers: Int = 0): NanoboardMessageData = {
    NanoboardMessageData(container, Some(message.parent), message.hash, message.text, answers, message.pow.toArray, message.signature.toArray)
  }

  def wrapDbPost[P <: Tables#DBPost](dbPost: P, answers: Int = 0): NanoboardMessageData = {
    NanoboardMessageData(Some(dbPost.containerId), Some(dbPost.parent), dbPost.hash, dbPost.text, answers, dbPost.pow.toArray, dbPost.signature.toArray)
  }

  // TODO: Remove unused
  def dbWrapMessage(message: NanoboardMessage, containerId: Long, firstSeen: Long = Instant.now().toEpochMilli): DBPost = {
    DBPost(message.hash, message.parent, message.text, firstSeen, containerId, message.pow, message.signature)
  }

  def wrapCategory(category: NanoboardCategory, answers: Int = 0): NanoboardMessageData = {
    NanoboardMessageData(None, None, category.hash, category.name, answers, Array.empty, Array.empty)
  }

  def unwrapToMessage(message: NanoboardMessageData): NanoboardMessage = {
    NanoboardMessage(message.parent.getOrElse(throw new IllegalArgumentException("Invalid parent hash")), message.text, ByteString(message.pow), ByteString(message.signature))
  }
}