package com.karasiq.nanoboard.captcha.storage

import akka.util.ByteString
import com.karasiq.nanoboard.captcha.NanoboardCaptcha
import com.karasiq.nanoboard.captcha.internal.Constants

import scala.concurrent.Future

/**
  * Nanoboard captcha memory storage
  * @param data Encoded captcha blocks
  */
final class NanoboardCaptchaMemorySource(data: ByteString) extends NanoboardCaptchaSource {
  override def apply(index: Int): Future[NanoboardCaptcha] = {
    assert(index < this.length, s"Invalid index: $index, captcha blocks: $length")
    val offset: Int = index * Constants.BLOCK_LENGTH
    val block: ByteString = data.slice(offset, offset + Constants.BLOCK_LENGTH)
    assert(block.length == Constants.BLOCK_LENGTH, "Invalid captcha block")
    Future.successful(NanoboardCaptcha.fromBytes(block))
  }

  override val length: Int = data.length / Constants.BLOCK_LENGTH
}
