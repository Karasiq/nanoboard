package com.karasiq.nanoboard.api

object NanoboardMessageData {
  def stripSignTags(text: String): String = {
    text.replaceAll("\\[(sign|pow)=[a-fA-F0-9]+\\]", "")
  }
}

case class NanoboardMessageData(containerId: Option[Long], parent: Option[String], hash: String, text: String, answers: Int) {
  def isSigned: Boolean = {
    "\\[sign=[a-fA-F0-9]+\\]".r.findFirstIn(text).isDefined
  }

  def textWithoutSign: String = {
    NanoboardMessageData.stripSignTags(text)
  }
}
