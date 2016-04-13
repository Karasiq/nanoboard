package com.karasiq.nanoboard.api

object NanoboardMessageData {
  val NO_POW = Array.fill[Byte](128)(0)
  val NO_SIGNATURE = Array.fill[Byte](64)(0)
}

case class NanoboardMessageData(containerId: Option[Long], parent: Option[String], hash: String, text: String, answers: Int, pow: Array[Byte] = NanoboardMessageData.NO_POW, signature: Array[Byte] = NanoboardMessageData.NO_SIGNATURE) {
  def isSigned: Boolean = {
    !signature.sameElements(NanoboardMessageData.NO_SIGNATURE)
  }

  def isCategory: Boolean = {
    parent.isEmpty
  }
}
