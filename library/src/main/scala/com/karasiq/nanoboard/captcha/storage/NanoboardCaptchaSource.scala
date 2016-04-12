package com.karasiq.nanoboard.captcha.storage

import java.io.{ByteArrayOutputStream, InputStream}

import akka.util.ByteString
import com.karasiq.nanoboard.captcha.NanoboardCaptcha
import org.apache.commons.io.IOUtils

import scala.concurrent.Future

/**
  * Nanoboard captcha source
  */
trait NanoboardCaptchaSource extends IndexedSeq[Future[NanoboardCaptcha]] {
  /**
    * Extracts the captcha block with specified index
    * @param index Captcha block index
    * @return Captcha block
    */
  def apply(index: Int): Future[NanoboardCaptcha]

  /**
    * Captcha blocks count
    */
  def length: Int
}

object NanoboardCaptchaSource {
  /**
    * Opens nanoboard captcha file storage
    * @param file File path
    * @return Nanoboard captcha file storage
    */
  def fromFile(file: String) = {
    new NanoboardCaptchaFileSource(file)
  }

  /**
    * Opens encoded nanoboard captcha storage
    * @param data Captcha storage data
    * @return Nanoboard captcha memory storage
    */
  def fromBytes(data: ByteString) = {
    new NanoboardCaptchaMemorySource(data)
  }

  /**
    * Reads input stream into memory and decodes as captcha storage
    * @param inputStream Input stream
    * @return Nanoboard captcha memory storage
    */
  def fromInputStream(inputStream: InputStream) = {
    val outputStream = new ByteArrayOutputStream()
    try {
      IOUtils.copyLarge(inputStream, outputStream)
      fromBytes(ByteString(outputStream.toByteArray))
    } finally {
      IOUtils.closeQuietly(outputStream)
    }
  }
}
