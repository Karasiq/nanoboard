package com.karasiq.nanoboard.test

import java.io.{ByteArrayInputStream, FileOutputStream}
import java.nio.file.{Files, Paths}

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.captcha.storage.NanoboardCaptchaSource
import com.karasiq.nanoboard.captcha.{NanoboardCaptcha, NanoboardPow}
import com.karasiq.nanoboard.encoding.NanoboardCrypto._
import com.karasiq.nanoboard.utils._
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
    NanoboardCaptcha.index(data, 10000) shouldBe 6430
  }

  it should "calculate valid hash" in {
    val powResult = Await.result(powCalculator.calculate(postPayload), Duration.Inf)
    val data = postPayload ++ powResult
    powCalculator.verify(data) shouldBe true
    println(data.utf8String)
  }

  "Captcha file" should "be parsed" in {
    val captchaFileName = CaptchaTest.unpackResource("test-captcha.nbc")
    val captchaFile = NanoboardCaptchaSource.fromFile(captchaFileName)
    try {
      val data = postPayload ++ preCalculatedPow
      val captcha = Await.result(captchaFile(NanoboardCaptcha.index(data, captchaFile.length)), Duration.Inf)
      val captchaPng = NanoboardCaptcha.render(captcha, 50, 20)
      // CaptchaTest.saveToFile(captchaPng, "test-captcha.png")
      sha256.digest(captchaPng).toHexString() shouldBe "da8b669b7aec1f57a4e4da7ad1afd59feea10b6305cc699951416f33ff4decde"
      sha256.digest(captcha.image).toHexString() shouldBe "007100217d1b9de3cf8cefb04eb2001149d985f01f84a5de86d6fe0ea2b893e3"
      captcha.publicKey.toHexString() shouldBe "90d643cec557ef34719d90a7a637dbcd3d14e6cfa59a3f3eb2f270065e320b6f"
      captcha.seed.toHexString() shouldBe "187295a2e886c05abbcedf5ea442b0b43d5fcd9532c9119043b1e7616865ea29"

      val signature = captcha.signature(data, "qxceo")
      signature.toHexString() shouldBe "d61ae209bd9a920309954d9d4b7c0a1f9ec2c569be09f1dc285ba25931e65002e9f9cf961a30cc680249ec336d9054ccabdddaef194d32e849c0c27ea77e2f03"
      captcha.verify(data, signature) shouldBe true

      val invalidSignature = captcha.signature(data, "11111")
      captcha.verify(data, invalidSignature) shouldBe false

      val signedText = ByteString(post.text) ++ preCalculatedPow ++ NanoboardCaptcha.wrapSignature(signature)
      sha256.digest(signedText).toHexString() shouldBe "2df1240bdffc504230096f3aa951586981a2c873af90fa14a434f22c7a086754"
      println(signedText)
      Await.result(NanoboardCaptcha.verify(post.copy(text = signedText.utf8String), powCalculator, captchaFile)(scala.concurrent.ExecutionContext.global), Duration.Inf) shouldBe true
    } finally {
      captchaFile.close()
      Files.delete(Paths.get(captchaFileName))
    }
  }
}
