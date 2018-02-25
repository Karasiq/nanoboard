package com.karasiq.nanoboard.test

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.util.ByteString
import org.scalatest.{FlatSpec, Matchers}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.NanoboardPow
import com.karasiq.nanoboard.captcha.impl.NanoboardPowV2
import com.karasiq.nanoboard.utils._

class NanoboardPowV2Test extends FlatSpec with Matchers {
  val post = NanoboardMessage("0" * 32, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
  val preCalculatedPow = ByteString.fromHexString("40ecb63c4168ceed2d018d0f756b8f07e66d4a31054db005766b56ad3f745c06e56fcf7f3030deb1d264cdba0af0b332ef11f83c951ed42a166f8d24efdcd010196b3acdeed8e6527b4fb45bcd15ef09ad16541322423d609019d76c2534680b3f3ab918caedfb45727548c3462ca29efe58a5c7f192bcb43995c071a907099e")
  val powCalculator = new NanoboardPowV2(3, 3, 1)(NanoboardPow.executionContext())

  "POW calculator" should "verify hash" in {
    val signedPost = post.copy(pow = preCalculatedPow)
    powCalculator.verify(post) shouldBe false
    powCalculator.verify(signedPost) shouldBe true
    powCalculator.getCaptchaIndex(signedPost, 18000) shouldBe 14104
  }

  it should "calculate valid hash" in {
    val powResult = Await.result(powCalculator.calculate(post), Duration.Inf)
    powCalculator.verify(post.copy(pow = powResult)) shouldBe true
  }
}
