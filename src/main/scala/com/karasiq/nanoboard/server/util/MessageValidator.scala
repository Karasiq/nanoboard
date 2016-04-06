package com.karasiq.nanoboard.server.util

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardCaptchaFile, NanoboardPow}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

private[server] object MessageValidator {
  def apply(captcha: NanoboardCaptchaFile, config: Config = ConfigFactory.load())(implicit ec: ExecutionContext): MessageValidator = {
    new MessageValidator(captcha, config)
  }
}

private[server] final class MessageValidator(captcha: NanoboardCaptchaFile, config: Config)(implicit ec: ExecutionContext) {
  private val requirePow = config.getBoolean("nanoboard.pow-required")
  private val maxPostSize = config.getMemorySize("nanoboard.max-post-size").toBytes
  private val spamFilter = config.getStringList("nanoboard.scheduler.spam-filter").toVector
  private val powCalculator = NanoboardPow(config)

  def isMessageValid(message: NanoboardMessage): Future[Boolean] = {
    Future.reduce(Seq(
      Future.successful(
        message.parent.matches(NanoboardMessage.HASH_FORMAT.regex) &&
          message.text.nonEmpty &&
          message.text.length <= maxPostSize &&
          spamFilter.forall(!message.text.matches(_))
      ),
      if (requirePow) NanoboardCaptcha.verify(message.text, powCalculator, captcha) else Future.successful(true)
    ))(_ && _)
  }
}
