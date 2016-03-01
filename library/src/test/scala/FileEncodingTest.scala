import java.io.InputStream
import javax.imageio.ImageIO

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.karasiq.nanoboard.encoding.{DataEncodingStage, NanoboardMessage}
import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class FileEncodingTest extends FlatSpec with Matchers {
  private def testImage(): InputStream = {
    getClass.getResourceAsStream("test-encoded.png")
  }

  private val pngEncoding = PngEncoding(_ ⇒ {
    val inputStream = testImage()
    val image = ImageIO.read(inputStream)
    inputStream.close()
    image
  })
  private val gzipCompression = GzipCompression()
  private val salsaCipher = SalsaCipher("nano")

  val testData = {
    val array = new Array[Byte](200000)
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
    val input = {
      val inputStream = testImage()
      val data = ByteString(IOUtils.toByteArray(inputStream))
      inputStream.close()
      data
    }

    val stage = Seq(gzipCompression, salsaCipher, pngEncoding)
    val decoded = stage.decode(input)
    decoded.length shouldBe 279352
    val encoded = stage.encode(decoded)
    assert(stage.decode(encoded) == decoded && decoded.utf8String.startsWith("0000270000aa000083006cc0000259003f3a0050ea0062150091cd000083003f8b0000800001170005ca00014500136000ea370000360000d8003b3c0034fd0000880000350032d500008c0000cb00007f002e4600005e00008e0000b70000d00000780000a100005e00008b00003700007100005e00226c9953dabbec38c625670087e8be5eca66[g]01/Mar/2016, 01:15:26 (UTC), client: nboard v1.7.13[/g]"))

    NanoboardMessage.parseMessages(decoded.utf8String).foreach(println)
  }
}
