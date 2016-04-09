import java.io.{ByteArrayInputStream, FileOutputStream}
import java.nio.file.{Files, Paths}

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardCaptchaFile, NanoboardPow}
import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

object CaptchaTest {
  def unpackResource(name: String): String = {
    val fileName = Files.createTempFile("captcha", ".nbc").toString
    val input = getClass.getClassLoader.getResourceAsStream(name)
    val output = new FileOutputStream(fileName)
    try {
      IOUtils.copyLarge(input, output)
      fileName
    } finally {
      IOUtils.closeQuietly(input)
      IOUtils.closeQuietly(output)
    }
  }

  def saveToFile(data: ByteString, fileName: String): Unit = {
    val input = new ByteArrayInputStream(data.toArray)
    val output = new FileOutputStream(fileName)
    try {
      IOUtils.copyLarge(input, output)
    } finally {
      IOUtils.closeQuietly(input)
      IOUtils.closeQuietly(output)
    }
  }
}

class CaptchaTest extends FlatSpec with Matchers {
  val post = NanoboardMessage("0" * 32, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
  val (postPayload, None) = NanoboardCaptcha.withoutSignature(post)
  val preCalculatedPow = ByteString("[pow=e02724de6a34a58555cece6475f5bc4ce4f957644ffa5c4fb983efc3bdd669651c5e5a8d4436930af26e1b6d0c9bf8ee2225ea2d446eec3ee3a7376a6788b579446a94581d02bfaa71eae8398d72397b77c3e0b152cee8999dc9129b7e72e6a98bfcfb164399a1ecd47fae574012fa3ea0d176638bae242085f0cd4ee0f0afb9]")
  val powCalculator = NanoboardPow()(NanoboardPow.executionContext())

  "POW calculator" should "verify hash" in {
    val data = postPayload ++ preCalculatedPow
    powCalculator.verify(postPayload) shouldBe false
    powCalculator.verify(data) shouldBe true
    powCalculator.captchaIndex(data, 10000) shouldBe 6430
  }

  it should "calculate valid hash" in {
    val powResult = Await.result(powCalculator.calculate(postPayload), Duration.Inf)
    val data = postPayload ++ powResult
    powCalculator.verify(data) shouldBe true
    println(data.utf8String)
  }

  "Captcha file" should "be parsed" in {
    val captchaFileName = CaptchaTest.unpackResource("test-captcha.nbc")
    val captchaFile = new NanoboardCaptchaFile(captchaFileName)
    try {
      val data = postPayload ++ preCalculatedPow
      val captcha = Await.result(captchaFile(powCalculator.captchaIndex(data, captchaFile.length)), Duration.Inf)
      val captchaPng = NanoboardCaptcha.render(captcha, 50, 20)
      // CaptchaTest.saveToFile(captchaPng, "test-captcha.png")
      captchaPng.hashCode() shouldBe 1579429469
      captcha.image.hashCode() shouldBe 196048800
      captcha.publicKey.hashCode() shouldBe 2074712280
      captcha.seed.hashCode() shouldBe 1681681311

      val signature = captcha.signature(data, "qxceo")
      captcha.verify(data, signature) shouldBe true

      val invalidSignature = captcha.signature(data, "11111")
      captcha.verify(data, invalidSignature) shouldBe false

      Await.result(NanoboardCaptcha.verify(post.copy(text = (ByteString(post.text) ++ preCalculatedPow ++ NanoboardCaptcha.wrapSignature(signature)).utf8String), powCalculator, captchaFile)(scala.concurrent.ExecutionContext.global), Duration.Inf) shouldBe true
    } finally {
      captchaFile.close()
      Files.delete(Paths.get(captchaFileName))
    }
  }
}
