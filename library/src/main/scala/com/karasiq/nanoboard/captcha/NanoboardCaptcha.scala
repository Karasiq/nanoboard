package com.karasiq.nanoboard.captcha

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.internal.{Constants, Ed25519}
import com.karasiq.nanoboard.captcha.storage.NanoboardCaptchaSource
import com.karasiq.nanoboard.encoding.NanoboardCrypto._
import com.karasiq.nanoboard.utils._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Nanoboard captcha block
  * @param publicKey EdDSA public key
  * @param seed XORed seed
  * @param image Encoded captcha image
  * @see [[https://github.com/nanoboard/nanoboard/commit/ef747596802919c270d0de61bd9bcdf319c787f0]]
  * @note {{{
  *   UnsignedMessage = ReplyTo + Text + PowValue
  *   Signature = Sign(UnsignedMessage, PrivateKeyFromSeed(DecryptedSeed))
  *   SignedMessage = UnsignedMessage + "[sign=$Hex(Signature)]"
  * }}}
  */
case class NanoboardCaptcha(publicKey: ByteString, seed: ByteString, image: ByteString) {
  require(publicKey.length == Constants.PUBLIC_KEY_LENGTH && seed.length == Constants.SEED_LENGTH && image.length == Constants.IMAGE_LENGTH, "Invalid data")

  def toBytes: ByteString = {
    publicKey ++ seed ++ image
  }

  private def decryptSeed(answer: String): ByteString = {
    val result = new Array[Byte](seed.length)
    val hashedAnswer = sha512.digest(ByteString(answer + publicKey.toHexString())) // Hex string is lowercase
    for (i ← seed.indices) {
      result(i) = (seed(i) ^ hashedAnswer(i & 63)).toByte
    }
    ByteString(result)
  }

  /**
    * Calculates the signature for post
    * @param post Unsigned message
    * @param guess Captcha answer
    * @return EdDSA digital signature (must be wrapped in `[sign]` tag)
    */
  def signature(post: ByteString, guess: String): ByteString = {
    val privateKey = Ed25519.privateKey(decryptSeed(guess))
    Ed25519.sign(privateKey, post)
  }

  /**
    * Verifies the signature for post
    * @param post Unsigned message
    * @param signature EdDSA digital signature
    * @return Is signature valid
    */
  def verify(post: ByteString, signature: ByteString): Boolean = {
    Ed25519.verify(Ed25519.publicKey(publicKey), post, signature)
  }
}

/**
  * Nanoboard captcha utility
  */
object NanoboardCaptcha {
  /**
    * Reads captcha block from bytes
    * @param byteString Encoded captcha block
    */
  def fromBytes(byteString: ByteString): NanoboardCaptcha = {
    assert(byteString.length == Constants.BLOCK_LENGTH, "Invalid captcha block length")
    NanoboardCaptcha(byteString.take(Constants.PUBLIC_KEY_LENGTH),
      byteString.drop(Constants.PUBLIC_KEY_LENGTH).take(Constants.SEED_LENGTH),
      byteString.drop(Constants.PUBLIC_KEY_LENGTH + Constants.SEED_LENGTH).take(Constants.IMAGE_LENGTH))
  }

  /**
    * Wraps signature in `[sign]` tag
    * @param signature EdDSA digital signature
    * @return Wrapped signature
    */
  def wrapSignature(signature: ByteString): ByteString = {
    ByteString(s"[sign=${signature.toHexString()}]")
  }

  /**
    * Adds the signature to message
    * @param message Unsigned message
    * @param signature EdDSA digital signature
    * @return Signed message
    */
  def withSignature(message: String, signature: ByteString): String = {
    message + wrapSignature(signature).utf8String
  }

  /**
    * Removes the signature from message
    * @param message Signed message
    * @return Unsigned message and EdDSA digital signature
    */
  def withoutSignature(message: NanoboardMessage): (ByteString, Option[ByteString]) = {
    val buffer = message.text.split("\\[sign=", 2)
    (ByteString(message.parent + buffer.head), if (buffer.length > 1) buffer(1).split("\\]", 2).headOption.map(ByteString.fromHexString) else None)
  }

  /**
    * Calculates captcha index
    * @param message Unsigned message
    * @param max Maximum captcha index
    * @return Index of the captcha to be solved
    */
  def index(message: ByteString, max: Int): Int = {
    sha256.digest(message).take(3).map(java.lang.Byte.toUnsignedInt) match {
      case Seq(b0, b1, b2) ⇒
        (b0 + b1 * 256 + b2 * 256 * 256) % max
    }
  }

  /**
    * Verifies POW and signature of the message
    * @param message Signed message
    * @param pow Proof-of-work calculator
    * @param captcha Captcha storage
    * @param ec Execution context
    * @return Is message valid
    */
  def verify(message: NanoboardMessage, pow: NanoboardPow, captcha: NanoboardCaptchaSource)(implicit ec: ExecutionContext): Future[Boolean] = {
    val (post, sign) = withoutSignature(message)
    Future.reduce(Seq(
      Future.successful(sign.isDefined && pow.verify(post)),
      captcha(index(post, captcha.length)).map(_.verify(post, sign.get))
    ))(_ && _)
  }

  private def renderBufferedImage(captcha: NanoboardCaptcha, width: Int = 50, height: Int = 20): BufferedImage = {
    var bii, byi = 0
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (x ← 0 until width; y ← 0 until height) {
      val color = if ((captcha.image(byi) & (1 << bii).toByte) != 0) Color.BLACK else Color.WHITE
      bii += 1
      if (bii >= 8) {
        bii = 0
        byi += 1
      }
      image.setRGB(x, y, color.getRGB)
    }
    image
  }

  /**
    * Renders encoded captcha image to png
    * @param captcha Encoded captcha image
    * @param width Output width
    * @param height Output height
    * @return Captcha image, rendered as png
    */
  def render(captcha: NanoboardCaptcha, width: Int = 50, height: Int = 20): ByteString = {
    val outputStream = new ByteArrayOutputStream()
    try {
      ImageIO.write(renderBufferedImage(captcha, width, height), "png", outputStream)
      ByteString(outputStream.toByteArray)
    } finally outputStream.close()
  }
}

