package com.karasiq.nanoboard.test

import java.nio.file.{Files, Paths}

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.util.ByteString
import org.scalatest.{FlatSpec, Matchers}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardPow}
import com.karasiq.nanoboard.captcha.impl.NanoboardPowV2
import com.karasiq.nanoboard.captcha.storage.NanoboardCaptchaSource
import com.karasiq.nanoboard.encoding.NanoboardCrypto._
import com.karasiq.nanoboard.test.utils.TestFiles
import com.karasiq.nanoboard.utils._

class CaptchaTest extends FlatSpec with Matchers {
  implicit val executionContext = NanoboardPow.executionContext()

  val post = NanoboardMessage("0" * 32, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
  val preCalculatedPow = ByteString.fromHexString("40ecb63c4168ceed2d018d0f756b8f07e66d4a31054db005766b56ad3f745c06e56fcf7f3030deb1d264cdba0af0b332ef11f83c951ed42a166f8d24efdcd010196b3acdeed8e6527b4fb45bcd15ef09ad16541322423d609019d76c2534680b3f3ab918caedfb45727548c3462ca29efe58a5c7f192bcb43995c071a907099e")
  val powCalculator = new NanoboardPowV2(3, 3, 1)
  
  "Captcha file" should "be parsed" in {
    val captchaFileName = TestFiles.unpackResource("test-captcha.nbc")
    val captchaFile = NanoboardCaptchaSource.fromFile(captchaFileName)
    try {
      val signedPost = post.copy(pow = preCalculatedPow)
      val signPayload = powCalculator.getSignPayload(signedPost)
      val captcha = Await.result(captchaFile(powCalculator.getCaptchaIndex(signedPost, captchaFile.length)), Duration.Inf)

      val captchaPng = NanoboardCaptcha.render(captcha, 50, 20)
      // CaptchaTest.saveToFile(captchaPng, "test-captcha.png")

      val validSignature = captcha.signature(signPayload, "sovyo")
      captcha.verify(signPayload, validSignature) shouldBe true

      val invalidSignature = captcha.signature(signPayload, "11111")
      captcha.verify(signPayload, invalidSignature) shouldBe false

      validSignature.toHexString() shouldBe "fcba6fa8b4d853a676dfa2033868276785ca2ed160333525fe1f506d4e1c60df5e52ad0f1d4c949e4bea14fddfc0c347ca4b16bbe2a87e55a57681d4fe04a303"
      sha256.digest(captchaPng).toHexString() shouldBe "7573466048bb005b5f04f3a198907b62d748c434230bd537afca1d39e135b5cc"
      sha256.digest(captcha.image).toHexString() shouldBe "55b95e7a9af5dcf2f48f0f417c59a72748fa8db7b141bdd78580443d9a64c413"
      captcha.publicKey.toHexString() shouldBe "7aed3dc8d009c749970356b64ddb445068fec933293a5cf56ff65a36d97e8079"
      captcha.seed.toHexString() shouldBe "fad5a58c217e0c6985e3241dd275bf667c47d3773b904e24cf0a39707741f815"

      Await.result(NanoboardCaptcha.verify(post.copy(pow = preCalculatedPow, signature = validSignature), powCalculator, captchaFile)(scala.concurrent.ExecutionContext.global), Duration.Inf) shouldBe true
    } finally {
      captchaFile.close()
      Files.delete(Paths.get(captchaFileName))
    }
  }
}
