import javax.imageio.ImageIO

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.{GzipCompression, NanoboardMessage, PngEncoding, SalsaCipher}
import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class FileEncodingTest extends FlatSpec with Matchers {
  val encoding = new PngEncoding(_ ⇒ {
    val inputStream = getClass.getResourceAsStream("test-image.png")
    val image = ImageIO.read(inputStream)
    inputStream.close()
    image
  })

  val testData = {
    val array = new Array[Byte](100)
    Random.nextBytes(array)
    ByteString(array)
  }

  "PNG encoder" should "encode data to png image" in {
    val encoded = encoding.encode(testData)
    val decoded = encoding.decode(encoded)
    decoded shouldBe testData
  }

  it should "decode ciphered data" in {
    val inputStream = getClass.getResourceAsStream("test-encoded.png")
    val data = encoding.decode(ByteString(IOUtils.toByteArray(inputStream)))
    inputStream.close()
    val cipher: SalsaCipher = new SalsaCipher("nano")
    val decoded = cipher.decode(data)
    val encoded = cipher.encode(decoded)
    assert(cipher.decode(encoded) == decoded)
    val decompressed = GzipCompression.decode(decoded)
    assert(decompressed.utf8String.startsWith("0000270000aa000083006cc0000259003f3a0050ea0062150091cd000083003f8b0000800001170005ca00014500136000ea370000360000d8003b3c0034fd0000880000350032d500008c0000cb00007f002e4600005e00008e0000b70000d00000780000a100005e00008b00003700007100005e00226c9953dabbec38c625670087e8be5eca66[g]01/Mar/2016, 01:15:26 (UTC), client: nboard v1.7.13[/g]"))

    NanoboardMessage.parseMessages(decompressed.utf8String).foreach(println)
  }
}
