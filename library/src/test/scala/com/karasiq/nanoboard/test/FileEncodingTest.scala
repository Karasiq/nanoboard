package com.karasiq.nanoboard.test

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.DataEncodingStage
import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.formats.{CBORMessagePackFormat, TextMessagePackFormat}
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.karasiq.nanoboard.test.utils.TestFiles
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

//noinspection ScalaDeprecation
class FileEncodingTest extends FlatSpec with Matchers {
  val testImage = TestFiles.resource("test-encoded.png")
  val testMessages = Vector(NanoboardMessage("0" * 32, "Test message 1"), NanoboardMessage("1" * 32, "Test message 2"))
  val pngEncoding = PngEncoding.fromEncodedImage(testImage)
  val gzipCompression = GzipCompression()
  val salsaCipher = SalsaCipher("nano")

  val testData = {
    val array = new Array[Byte](50000)
    Random.nextBytes(array)
    ByteString(array)
  }

  "PNG encoder" should "encode data to png image" in {
    val encoded = pngEncoding.encode(testData)
    val decoded = pngEncoding.decode(encoded)
    decoded shouldBe testData
  }

  it should "compress and encrypt data" in {
    val stages = Seq[DataEncodingStage](
      gzipCompression,
      salsaCipher,
      Seq(gzipCompression, salsaCipher)
    )

    stages.foreach { stage ⇒
      println(s"Testing stage: $stage")
      val encoded = stage.encode(testData)
      val decoded = stage.decode(encoded)
      decoded shouldBe testData
    }
  }

  it should "decode ciphered data" in {
    val stage = Seq(gzipCompression, salsaCipher, pngEncoding)
    val decoded = stage.decode(testImage)
    decoded.length shouldBe 279352
    val encoded = stage.encode(decoded)
    assert(stage.decode(encoded) == decoded && decoded.utf8String.startsWith("0000270000aa000083006cc0000259003f3a0050ea0062150091cd000083003f8b0000800001170005ca00014500136000ea370000360000d8003b3c0034fd0000880000350032d500008c0000cb00007f002e4600005e00008e0000b70000d00000780000a100005e00008b00003700007100005e00226c9953dabbec38c625670087e8be5eca66[g]01/Mar/2016, 01:15:26 (UTC), client: nboard v1.7.13[/g]"))

    NanoboardMessage.parseMessages(decoded).foreach(println)
  }

  "Message pack" should "be encoded in text format" in {
    val result = TextMessagePackFormat.writeMessages(testMessages)
    TextMessagePackFormat.parseMessages(result) shouldBe testMessages
    result.hashCode() shouldBe -488225130
    println(result.utf8String)
    // TestFiles.saveToFile(result, "text-test.txt")
  }

  it should "be encoded in CBOR format" in {
    val result = CBORMessagePackFormat.writeMessages(testMessages)
    CBORMessagePackFormat.parseMessages(result) shouldBe testMessages
    result.hashCode() shouldBe 652261416
    // TestFiles.saveToFile(result, "cbor-test.bin")
  }
}
