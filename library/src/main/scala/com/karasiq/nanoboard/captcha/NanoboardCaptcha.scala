package com.karasiq.nanoboard.captcha

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.imageio.ImageIO

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataCipher
import org.apache.commons.codec.binary.Hex

import scala.concurrent.{ExecutionContext, Future}

/**
  * Nanoboard captcha block
  * @param publicKey EdDSA public key
  * @param seed XORed seed
  * @param image Encoded captcha image
  * @see [[https://github.com/nanoboard/nanoboard/commit/ef747596802919c270d0de61bd9bcdf319c787f0]]
  */
case class NanoboardCaptcha(publicKey: ByteString, seed: ByteString, image: ByteString) {
  require(publicKey.length == 32 && seed.length == 32 && image.length == 125)

  def toBytes: ByteString = {
    publicKey ++ seed ++ image
  }

  private def decryptSeed(answer: String): ByteString = {
    val sha512 = MessageDigest.getInstance("SHA512", DataCipher.provider)
    val result = new Array[Byte](32)
    val array = sha512.digest(ByteString(answer + Hex.encodeHexString(publicKey.toArray)).toArray) // Hex string is lowercase
    for (i ← seed.indices) {
      result(i) = (seed(i) ^ array(i & 63)).toByte
    }
    ByteString(result)
  }

  /**
    * Calculates the signature for post
    * @param post Message without the `[sign]` tag
    * @param guess Captcha answer
    * @return EdDSA digital signature (must be wrapped in `[sign]` tag)
    */
  def signature(post: ByteString, guess: String): ByteString = {
    val privateKey = Ed25519.privateKey(decryptSeed(guess))
    Ed25519.sign(privateKey, post)
  }

  /**
    * Verifies the signature for post
    * @param post Message without the `[sign]` tag
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
    assert(byteString.length == 32 + 32 + 125, "Invalid captcha block length")
    NanoboardCaptcha(byteString.take(32), byteString.drop(32).take(32), byteString.drop(64).take(125))
  }

  /**
    * Adds the signature to message
    * @param message Unsigned message
    * @param signature EdDSA digital signature
    * @return Signed message
    */
  def withSignature(message: String, signature: ByteString): String = {
    message + s"[sign=${Hex.encodeHexString(signature.toArray)}]"
  }

  /**
    * Removes the signature from message
    * @param message Signed message
    * @return Unsigned message and EdDSA digital signature
    */
  def withoutSignature(message: String): (ByteString, Option[ByteString]) = {
    val buffer = message.split("\\[sign=", 2)
    (ByteString(buffer.head), if (buffer.length > 1) buffer(1).split("\\]", 2).headOption.map(str ⇒ ByteString(Hex.decodeHex(str.toCharArray))) else None)
  }

  /**
    * Verifies POW and signature of the message
    * @param message Signed message
    * @param pow Proof-of-work calculator
    * @param captcha Captcha storage
    * @param ec Execution context
    * @return Is message valid
    */
  def verify(message: String, pow: NanoboardPow, captcha: NanoboardCaptchaFile)(implicit ec: ExecutionContext): Future[Boolean] = {
    val (post, sign) = withoutSignature(message)
    Future.reduce(Seq(
      Future.successful(sign.isDefined && pow.verify(post)),
      captcha(pow.captchaIndex(post, captcha.length)).map(_.verify(post, sign.get))
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

