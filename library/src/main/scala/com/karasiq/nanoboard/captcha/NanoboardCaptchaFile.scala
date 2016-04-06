package com.karasiq.nanoboard.captcha

import java.io.{Closeable, RandomAccessFile}
import java.util.concurrent.Executors

import akka.util.ByteString
import org.apache.commons.io.IOUtils

import scala.concurrent.{ExecutionContext, Future}

object NanoboardCaptchaFile {
  /**
    * Opens nanoboard captcha file storage
    * @param file File path
    * @return Nanoboard captcha file storage
    */
  def apply(file: String): NanoboardCaptchaFile = {
    new NanoboardCaptchaFile(file)
  }
}

/**
  * Nanoboard captcha file storage
  * @param file File path
  */
final class NanoboardCaptchaFile(file: String) extends Closeable {
  private implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
  private val randomAccessFile = new RandomAccessFile(file, "r")

  private val captchaLength = 189
  private val buffer = new Array[Byte](captchaLength)

  /**
    * Extracts the captcha block with specified index
    * @param index Captcha block index
    * @return Captcha block
    */
  def apply(index: Int): Future[NanoboardCaptcha] = {
    try {
      assert(index < this.length, s"Invalid index: $index, captcha blocks: $length")
      Future {
        randomAccessFile.seek(index.toLong * captchaLength)
        randomAccessFile.read(buffer, 0, captchaLength)
        NanoboardCaptcha.fromBytes(ByteString(buffer))
      }
    } catch {
      case e: Throwable ⇒
        Future.failed(e)
    }
  }

  /**
    * Captcha blocks count
    */
  val length: Int = {
    val length = randomAccessFile.length() / captchaLength
    math.min(length, Int.MaxValue).toInt
  }

  /**
    * Closes this file
    */
  override def close(): Unit = {
    ec.shutdownNow()
    IOUtils.closeQuietly(randomAccessFile)
  }
}
