package com.karasiq.nanoboard.test

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.util.ByteString
import org.scalatest.{FlatSpec, Matchers}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.NanoboardPow
import com.karasiq.nanoboard.captcha.impl.NanoboardPowV1
import com.karasiq.nanoboard.captcha.storage.NanoboardCaptchaSource
import com.karasiq.nanoboard.utils._

class NanoboardPowV1Test extends FlatSpec with Matchers {
  val testMessage = NanoboardMessage("cd94a3d60f2f521806abebcd3dc3f549", "[g]Wed, 11 Jan 2017 18:25:58 GMT, client: 3.0[/g]\nКто-нибудь знает как убрать эту ужасную капчу?[xmg=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]")

  val signedMessage = testMessage.copy(
    pow = ByteString.fromHexString("f3e72c50e82e8acc2c6e48bb40c4f48ecf35ddaf67a2713a133ef177616b2df59e3ba87af376d4c9fc011168f49c2ffb23aaaa1609f2d95ab535f721dbb117c1af8620c2f4c9a55dfda52a25ccd58fd311e704c2df0da8a1078235c224d9c61c25f14bb42db98c344b207b6a418568645eaa8f55cdc362eff6f5f5d464974511"),
    signature = ByteString.fromHexString("1280afd103d74232f3b739365c42054d9eafd65917b1b9cbd7767a550f26b4c7c923f436b9ff220c70f20e6cf6da8acb65322244125de50101d75860fd49d605")
  )

  val powCalculator = new NanoboardPowV1(3, 3, 1)(NanoboardPow.executionContext())

  "POW calculator" should "validate hash" in {
    powCalculator.verify(signedMessage) shouldBe true
    powCalculator.getCaptchaIndex(signedMessage, 18000) shouldBe 2887
  }

  it should "generate valid hash" in {
    val powResult = Await.result(powCalculator.calculate(testMessage), Duration.Inf)
    powCalculator.verify(testMessage.copy(pow = powResult)) shouldBe true
  }

  ignore should "verify signature" in {
    val captchaFile = NanoboardCaptchaSource.fromFile("C:\\Users\\User\\.nanoboard\\21eab02c.nbc")
    val captcha = Await.result(captchaFile(powCalculator.getCaptchaIndex(signedMessage, captchaFile.length)), Duration.Inf)
    captcha.verify(powCalculator.getSignPayload(signedMessage), signedMessage.signature) shouldBe true
  }
}
