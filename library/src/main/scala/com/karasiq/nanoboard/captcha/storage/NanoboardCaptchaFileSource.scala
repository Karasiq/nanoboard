package com.karasiq.nanoboard.captcha.storage

import java.io.{Closeable, RandomAccessFile}
import java.util.concurrent.Executors

import akka.util.ByteString
import com.karasiq.nanoboard.captcha.NanoboardCaptcha
import com.karasiq.nanoboard.captcha.internal.Constants
import org.apache.commons.io.IOUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Nanoboard captcha file storage
  * @param file File path
  */
final class NanoboardCaptchaFileSource(file: String) extends NanoboardCaptchaSource with Closeable {
  private implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
  private val randomAccessFile = new RandomAccessFile(file, "r")
  private val buffer = new Array[Byte](Constants.BLOCK_LENGTH)

  def apply(index: Int): Future[NanoboardCaptcha] = {
    try {
      assert(index < this.length, s"Invalid index: $index, captcha blocks: $length")
      Future {
        randomAccessFile.seek(index.toLong * Constants.BLOCK_LENGTH)
        randomAccessFile.read(buffer, 0, Constants.BLOCK_LENGTH)
        NanoboardCaptcha.fromBytes(ByteString(buffer))
      }
    } catch {
      case e: Throwable ⇒
        Future.failed(e)
    }
  }

  override val length: Int = {
    val length = randomAccessFile.length() / Constants.BLOCK_LENGTH
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
