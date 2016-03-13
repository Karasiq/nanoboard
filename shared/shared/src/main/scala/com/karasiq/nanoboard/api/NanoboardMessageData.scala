package com.karasiq.nanoboard.api

case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)
