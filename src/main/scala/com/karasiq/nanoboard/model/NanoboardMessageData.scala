package com.karasiq.nanoboard.model

import com.karasiq.nanoboard.NanoboardMessage

import scala.language.implicitConversions

case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

object NanoboardMessageData {
  implicit def messageToMessageData(message: NanoboardMessage): NanoboardMessageData = {
    NanoboardMessageData(Some(message.parent), message.hash, message.text, 0)
  }

  implicit def messageDataToMessage(messageData: NanoboardMessageData): NanoboardMessage = {
    NanoboardMessage(messageData.parent.get, messageData.text)
  }
}
