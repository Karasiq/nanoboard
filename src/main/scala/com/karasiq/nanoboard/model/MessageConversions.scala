package com.karasiq.nanoboard.model

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.api.NanoboardMessageData

import scala.language.implicitConversions

object MessageConversions {
  implicit def messageToMessageData(message: NanoboardMessage): NanoboardMessageData = {
    NanoboardMessageData(Some(message.parent), message.hash, message.text, 0)
  }

  implicit def messageDataToMessage(messageData: NanoboardMessageData): NanoboardMessage = {
    NanoboardMessage(messageData.parent.get, messageData.text)
  }
}