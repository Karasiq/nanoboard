package com.karasiq.nanoboard.captcha

import java.util.concurrent.{Executors, RejectedExecutionException}

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataCipher.{BCDigestOps, sha256}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.binary.Hex
import org.bouncycastle.crypto.digests.SHA256Digest

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

object NanoboardPow {
  /**
    * Creates nanoboard POW calculator from config
    * @param config Configuration object
    * @return Proof-of-work calculator
    */
  def apply(config: Config = ConfigFactory.load()): NanoboardPow = {
    new NanoboardPow(config.getInt("nanoboard.pow.offset"), config.getInt("nanoboard.pow.length"), config.getInt("nanoboard.pow.threshold"))
  }
}

/**
  * Nanoboard proof-of-work calculator
  * @param offset Hash offset for bytes verification
  * @param length Required consequent bytes
  * @param threshold Maximum byte value
  * @see [[https://github.com/nanoboard/nanoboard/commit/ef747596802919c270d0de61bd9bcdf319c787f0]]
  */
final class NanoboardPow(offset: Int, length: Int, threshold: Int) {
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
    * @param message Message with `[pow]` tag
    * @return Is POW valid
    */
  def verify(message: ByteString): Boolean = {
    verify(message, sha256)
  }

  /**
    * Calculates captcha index
    * @param message Message with `[pow]` tag
    * @param max Maximum captcha index
    * @return Index of the captcha to be solved
    */
  def captchaIndex(message: ByteString, max: Int): Int = {
    sha256.digest(message).take(3).map(java.lang.Byte.toUnsignedInt) match {
      case Seq(b0, b1, b2) ⇒
        (b0 + b1 * 256 + b2 * 256 * 256) % max
    }
  }

  /**
    * Calculates nanoboard proof-of-work value
    * @param message Message without a `[pow]` tag
    * @return `[pow]` tag to be appended to message
    */
  def calculate(message: ByteString): Future[ByteString] = {
    implicit val powContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()))
    val open = ByteString("[pow=")
    val preHashed = sha256.updated(message ++ open)
    val close = ByteString("]")
    val result = Promise[ByteString]

    def submitTasks(): Unit = {
      try {
        Future.fold(for (_ ← 0 to 100) yield Future {
          val array = Array.ofDim[Byte](128)
          Random.nextBytes(array)
          val data = ByteString(Hex.encodeHexString(array)) ++ close
          if (verify(data, new SHA256Digest(preHashed))) {
            result.success(open ++ data)
            powContext.shutdownNow()
          }
        })(())((_, _) ⇒ ()).foreach { _ ⇒
          if (!result.isCompleted && !powContext.isShutdown) {
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
