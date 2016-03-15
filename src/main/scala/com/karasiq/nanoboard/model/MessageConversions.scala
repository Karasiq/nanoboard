package com.karasiq.nanoboard.model

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.api.NanoboardMessageData

import scala.language.implicitConversions

private[nanoboard] object MessageConversions {
  def asMessageData(message: NanoboardMessage, container: Option[Long] = None, answers: Int = 0): NanoboardMessageData = {
    NanoboardMessageData(container, Some(message.parent), message.hash, message.text, answers)
  }

  implicit def messageToMessageData(message: NanoboardMessage): NanoboardMessageData = {
    asMessageData(message)
  }

  implicit def messageDataToMessage(message: NanoboardMessageData): NanoboardMessage = {
    NanoboardMessage(message.parent.getOrElse(throw new IllegalArgumentException("Invalid parent hash")), message.text)
  }
}