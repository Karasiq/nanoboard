package com.karasiq.nanoboard.captcha

import java.security.MessageDigest
import java.util.concurrent.{Executors, RejectedExecutionException}
import java.util.function.Supplier

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataCipher
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.binary.Hex

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
  private val threadLocalSha256 = ThreadLocal.withInitial(new Supplier[MessageDigest] {
    override def get(): MessageDigest = DataCipher.sha256
  })

  private def sha256 = {
    val md = threadLocalSha256.get()
    md.reset()
    md
  }

  /**
    * Verifies the message proof-of-work value
    * @param message Message with `[pow]` tag
    * @return Is POW valid
    */
  def verify(message: ByteString): Boolean = {
    val hash = sha256.digest(message.toArray[Byte])
    var maxLength = 0
    for (i ← offset until hash.length if maxLength < this.length) {
      if (java.lang.Byte.toUnsignedInt(hash(i)) <= threshold) {
        maxLength += 1
      } else {
        maxLength = 0
      }
    }
    maxLength >= this.length
  }

  /**
    * Calculates captcha index
    * @param message Message with `[pow]` tag
    * @param max Maximum captcha index
    * @return Index of the captcha to be solved
    */
  def captchaIndex(message: ByteString, max: Int): Int = {
    sha256.digest(message.toArray[Byte]).take(3).map(java.lang.Byte.toUnsignedInt) match {
      case Array(b0, b1, b2) ⇒
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
    val close = ByteString("]")
    val result = Promise[ByteString]

    def submitTasks(): Unit = {
      try {
        Future.fold(for (_ ← 0 to 100) yield Future {
          val array = Array.ofDim[Byte](128)
          Random.nextBytes(array)
          val data = open ++ ByteString(Hex.encodeHexString(array)) ++ close
          if (verify(message ++ data)) {
            result.success(data)
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
