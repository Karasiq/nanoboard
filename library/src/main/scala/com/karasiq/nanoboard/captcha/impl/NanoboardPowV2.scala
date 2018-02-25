package com.karasiq.nanoboard.captcha.impl

import java.util.concurrent.RejectedExecutionException

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

import akka.util.ByteString
import org.bouncycastle.crypto.digests.SHA256Digest

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.NanoboardPow
import com.karasiq.nanoboard.encoding.NanoboardCrypto.{sha256, BCDigestOps}

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
final class NanoboardPowV2(offset: Int, length: Int, threshold: Int)(implicit ec: ExecutionContext) extends NanoboardPow {
  // For verification with cached SHA256 state
  private def verify(bytes: ByteString, md: SHA256Digest): Boolean = {
    val hash = md.digest(bytes)
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
  def verify(message: NanoboardMessage): Boolean = {
    verify(getSignPayload(message), sha256)
  }

  /**
    * Calculates nanoboard proof-of-work value
    * @param message Message without a calculated POW
    * @return Proof-of-work value
    */
  def calculate(message: NanoboardMessage): Future[ByteString] = {
    val payload = getPOWPayload(message)
    val preHashed = sha256.updated(payload)
    val result = Promise[ByteString]

    def submitTasks(): Unit = {
      try {
        Future.sequence(for (_ ← 0 to 100) yield Future {
          val array = Array.ofDim[Byte](NanoboardMessage.POWLength)
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

  def getCaptchaIndex(message: NanoboardMessage, max: Int): Int = {
    sha256.digest(getSignPayload(message)).take(3).map(java.lang.Byte.toUnsignedInt) match {
      case Seq(b0, b1, b2) ⇒
        (b0 + b1 * 256 + b2 * 256 * 256) % max
    }
  }

  def getSignPayload(message: NanoboardMessage): ByteString = {
    getPOWPayload(message) ++ message.pow
  }

  private[this] def getPOWPayload(message: NanoboardMessage) = {
    ByteString(message.parent + message.text)
  }
}
