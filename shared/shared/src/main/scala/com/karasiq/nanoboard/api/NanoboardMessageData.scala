package com.karasiq.nanoboard.api

case class NanoboardMessageData(containerId: Option[Long], parent: Option[String], hash: String, text: String, answers: Int)
