package com.karasiq.nanoboard.api

case class NanoboardMessageData(containerId: Option[Long], parent: Option[String], hash: String, text: String, answers: Int) {
  def isSigned: Boolean = {
    "\\[sign=[a-fA-F0-9]+\\]".r.findFirstIn(text).isDefined
  }

  def textWithoutSign: String = {
    text.replaceAll("\\[(sign|pow)=[a-fA-F0-9]+\\]", "")
  }
}
