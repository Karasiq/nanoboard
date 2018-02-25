package com.karasiq.nanoboard.server.utils

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.{Config, ConfigFactory}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardPow}
import com.karasiq.nanoboard.captcha.storage.NanoboardCaptchaSource

private[server] object MessageValidator {
  def apply(captcha: NanoboardCaptchaSource, config: Config = ConfigFactory.load())(implicit ec: ExecutionContext): MessageValidator = {
    new MessageValidator(captcha, config)
  }
}

private[server] final class MessageValidator(captcha: NanoboardCaptchaSource, config: Config)(implicit ec: ExecutionContext) {
  private[this] val requirePow = config.getBoolean("nanoboard.pow-required")
  private[this] val maxPostSize = config.getMemorySize("nanoboard.max-post-size").toBytes
  private[this] val spamFilter = config.getStringList("nanoboard.scheduler.spam-filter").toVector
  private[this] val powCalculator = NanoboardPow(config)

  def isMessageValid(message: NanoboardMessage): Future[Boolean] = {
    Future.reduce(Seq(
      Future.successful(
        message.parent.matches(NanoboardMessage.HashFormat.regex) &&
          message.text.nonEmpty &&
          message.text.length <= maxPostSize &&
          spamFilter.forall(!message.text.matches(_))
      ),
      if (requirePow) NanoboardCaptcha.verify(message, powCalculator, captcha) else Future.successful(true)
    ))(_ && _)
  }
}
