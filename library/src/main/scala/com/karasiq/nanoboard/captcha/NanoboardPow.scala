package com.karasiq.nanoboard.captcha

import java.util.concurrent.{Executors, RejectedExecutionException}

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.NanoboardCrypto.{BCDigestOps, sha256}
import com.typesafe.config.{Config, ConfigFactory}
import org.bouncycastle.crypto.digests.SHA256Digest

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

object NanoboardPow {
  /**
    * Creates nanoboard POW calculator from config
    * @param config Configuration object
    * @return Proof-of-work calculator
    */
  def apply(config: Config = ConfigFactory.load())(implicit ec: ExecutionContext): NanoboardPow = {
    new NanoboardPow(config.getInt("nanoboard.pow.offset"), config.getInt("nanoboard.pow.length"), config.getInt("nanoboard.pow.threshold"))
  }

  /**
    * Provides execution context, optimised for nanoboard proof-of-work calculation
    * @return Work stealing pool execution context
    */
  def executionContext() = {
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(Runtime.getRuntime.availableProcessors()))
  }

  /**
    * Data fragment to calculate POW value
    * @param message Message
    * @return ReplyTo + Text
    */
  def dataToPow(message: NanoboardMessage): ByteString = {
    ByteString(message.parent + message.text)
  }
}

/**
  * Nanoboard proof-of-work calculator
  * @param offset Hash offset for bytes verification
  * @param length Required consequent bytes
  * @param threshold Maximum byte value
  * @see [[https://github.com/nanoboard/nanoboard/commit/ef747596802919c270d0de61bd9bcdf319c787f0]]
  * @note {{{
  *   PowValue = RandomBytes(128)
  *   PowHash = SHA256(ReplyTo + Text + PowValue)
  * }}}
  */
final class NanoboardPow(offset: Int, length: Int, threshold: Int)(implicit ec: ExecutionContext) {
  // For verification with cached SHA256 state
  private def verify(update: ByteString, md: SHA256Digest): Boolean = {
    val hash = md.digest(update)
    var maxLength = 0
    for (i ← offset until hash.length if maxLength < this.length) {
      if (java.lang.Byte.toUnsignedInt(hash(i)) <= threshold)
        maxLength += 1
      else
        maxLength = 0
    }
    maxLength >= this.length
  }

  /**
    * Verifies the message proof-of-work value
    * @param message Message with calculated POW
    * @return Is POW valid
    */
  def verify(message: ByteString): Boolean = {
    verify(message, sha256)
  }

  /**
    * Calculates nanoboard proof-of-work value
    * @param message Message without a calculated POW
    * @return Proof-of-work value
    */
  def calculate(message: ByteString): Future[ByteString] = {
    val preHashed = sha256.updated(message)
    val result = Promise[ByteString]

    def submitTasks(): Unit = {
      try {
        Future.sequence(for (_ ← 0 to 100) yield Future {
          val array = Array.ofDim[Byte](NanoboardMessage.POW_LENGTH)
          Random.nextBytes(array)
          val data = ByteString(array)
          if (verify(data, new SHA256Digest(preHashed))) {
            result.success(data)
          }
        }).foreach { _ ⇒
          if (!result.isCompleted) {
            submitTasks()
          }
        }
      } catch {
        case _: RejectedExecutionException ⇒
          // Pass

        case e: Throwable ⇒
          result.failure(e)
      }
    }

    submitTasks()
    result.future
  }
}
