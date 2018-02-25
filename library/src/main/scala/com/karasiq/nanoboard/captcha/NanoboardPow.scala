package com.karasiq.nanoboard.captcha

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, Future}

import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.impl.{NanoboardPowV1, NanoboardPowV2}

trait NanoboardPow {
  /**
    * Verifies the message proof-of-work value
    * @param message Message with calculated POW
    * @return Is POW valid
    */
  def verify(message: NanoboardMessage): Boolean

  /**
    * Calculates nanoboard proof-of-work value
    * @param message Message without a calculated POW
    * @return Proof-of-work value
    */
  def calculate(message: NanoboardMessage): Future[ByteString]

  /**
    * Calculates captcha index
    * @param message Unsigned message
    * @param max Maximum captcha index
    * @return Index of the captcha to be solved
    */
  def getCaptchaIndex(message: NanoboardMessage, max: Int): Int

  /**
    * Data to sign/verify with the EdDSA digital signature
    * @param message Message
    * @return ReplyTo + Text + PowValue
    */
  def getSignPayload(message: NanoboardMessage): ByteString
}

object NanoboardPow {
  /**
    * Creates nanoboard POW calculator from config
    * @param config Configuration object
    * @return Proof-of-work calculator
    */
  def apply(config: Config = ConfigFactory.load())(implicit ec: ExecutionContext): NanoboardPow = {
    val offset = config.getInt("nanoboard.pow.offset")
    val length = config.getInt("nanoboard.pow.length")
    val threshold = config.getInt("nanoboard.pow.threshold")

    config.getString("nanoboard.pow.version") match {
      case "v1" ⇒
        new NanoboardPowV1(offset, length, threshold)

      case "v2" ⇒
        new NanoboardPowV2(offset, length, threshold)
    }
  }

  /**
    * Provides execution context, optimised for nanoboard proof-of-work calculation
    * @return Work stealing pool execution context
    */
  def executionContext() = {
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(Runtime.getRuntime.availableProcessors()))
  }
}