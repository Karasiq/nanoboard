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
  private val initialPosts = Set("bdd4b5fc1b3a933367bc6830fef72a35", "cd94a3d60f2f521806abebcd3dc3f549", "f682830a470200d738d32c69e6c2b8a4")

  def isMessageValid(message: NanoboardMessage): Future[Boolean] = {
    Future.reduce(Seq(
      Future.successful(
        message.parent.matches(NanoboardMessage.HASH_FORMAT.regex) &&
          message.text.nonEmpty &&
          message.text.length <= maxPostSize &&
          spamFilter.forall(!message.text.matches(_))
      ),
      if (requirePow && !initialPosts.contains(message.hash)) NanoboardCaptcha.verify(message, powCalculator, captcha) else Future.successful(true)
    ))(_ && _)
  }
}
