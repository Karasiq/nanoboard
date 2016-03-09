package com.karasiq.nanoboard.server.util

import com.karasiq.nanoboard.NanoboardMessage
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._

private[server] object MessageValidator {
  def apply(config: Config = ConfigFactory.load()): MessageValidator = {
    new MessageValidator(config)
  }
}

private[server] final class MessageValidator(config: Config) {
  private val maxPostSize = config.getMemorySize("nanoboard.max-post-size").toBytes
  private val spamFilter = config.getStringList("nanoboard.scheduler.spam-filter").toVector

  def isMessageValid(message: NanoboardMessage): Boolean = {
    message.parent.matches(NanoboardMessage.hashRegex.regex) &&
      message.text.nonEmpty &&
      message.text.length <= maxPostSize &&
      spamFilter.forall(!message.text.matches(_))
  }
}
