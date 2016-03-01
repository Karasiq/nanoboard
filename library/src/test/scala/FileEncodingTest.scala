import java.io.{File, FileInputStream, FileOutputStream}
import java.util.Date

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.PngEncoding
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class FileEncodingTest extends FlatSpec with Matchers {
  val tempFile = File.createTempFile("encoding-test", new Date().getTime.toString)

  val testData = {
    val array = new Array[Byte](100)
    Random.nextBytes(array)
    ByteString(array)
  }

  "PNG encoder" should "encode png image" in {
    val inputStream = getClass.getResourceAsStream("testimage.png")
    val outputStream = new FileOutputStream(tempFile)
    PngEncoding.encode(inputStream, testData, outputStream)
    inputStream.close()
    outputStream.close()
  }

  it should "decode png image" in {
    val inputStream = new FileInputStream(tempFile)
    PngEncoding.decode(inputStream) shouldBe testData
    inputStream.close()
  }
}
