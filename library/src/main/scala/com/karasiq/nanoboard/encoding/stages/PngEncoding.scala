package com.karasiq.nanoboard.encoding.stages

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteOrder
import java.util
import javax.imageio.ImageIO

import akka.util.ByteString
import com.karasiq.nanoboard.encoding.DataEncodingStage

object PngEncoding {
  /**
    * Creates PNG encoder
    * @param sourceImage Source image provider
    * @return PNG encoder
    */
  def apply(sourceImage: ByteString ⇒ BufferedImage): PngEncoding = {
    new PngEncoding(sourceImage)
  }

  /**
    * Creates PNG encoder, that uses single image for encoding
    * @param imageData Encoded image, must be readable with [[javax.imageio.ImageIO#read(java.io.InputStream) Java ImageIO]]
    * @return PNG encoder
    */
  def fromEncodedImage(imageData: ByteString): PngEncoding = {
    apply { data ⇒
      val inputStream = new ByteArrayInputStream(imageData.toArray)
      val image = try { ImageIO.read(inputStream) } finally inputStream.close()
      assert(image.ne(null), "Invalid image")
      assert(PngEncoding.imageBytes(image) >= data.length, s"Image is too small, ${data.length} bits required")
      image
    }
  }

  /**
    * Default PNG decoder
    * @note Would fail on encode request
    */
  val decoder = apply(_ ⇒ null)

  /**
    * Image bytes, required to encode provided data
    * @param data Payload
    * @return Required image length
    */
  def requiredSize(data: ByteString): Int = {
    (4 + data.length) * 8
  }

  /**
    * Available image size
    * @param image Source image
    * @return Actual image length
    */
  def imageBytes(image: BufferedImage): Int = {
    image.getWidth * image.getHeight * 3
  }
}

/**
  * PNG steganography encoding stage. Hides provided data in image color bits.
  * @param sourceImage Source image provider
  * @see [[http://blog.andersen.im/2014/11/hiding-your-bits-in-the-bytes/ Original algorithm]]
  */
final class PngEncoding(sourceImage: ByteString ⇒ BufferedImage) extends DataEncodingStage {
  private implicit val byteOrder = ByteOrder.LITTLE_ENDIAN

  @inline
  private def asInt(bytes: ByteString): Int = {
    bytes.padTo(4, 0.toByte).toByteBuffer.order(byteOrder).getInt
  }

  @inline
  private def asBytes(int: Int): ByteString = {
    ByteString.newBuilder
      .putInt(int)
      .result()
  }

  /**
    * Decodes hidden data from RGB array
    * @param bytes RGB array
    * @param dataLength Data length
    * @param index Data offset
    * @return Extracted data
    */
  private def readBytes(bytes: Array[Int], dataLength: Int, index: Int): ByteString = {
    val bitCount = dataLength * 8
    val offset = index * 8
    val required: Int = offset + bitCount - 1
    assert(bytes.length >= required, s"Invalid data length, $required bytes required")
    val result = new util.BitSet(bitCount)
    for (i ← 0 until bitCount) {
      result.set(i, bytes(offset + i) % 2 == 1)
    }
    ByteString(result.toByteArray).take(dataLength)
  }

  /**
    * Decodes image to RGB array.
    * Output format: {{{Array(red, green, blue)}}}
    * @param img Source image
    * @return RGB array
    */
  @inline
  private def asRgbBytes(img: BufferedImage): Array[Int] = {
    val colors = new Array[Int](img.getWidth * img.getHeight)
    img.getRGB(0, 0, img.getWidth, img.getHeight, colors, 0, img.getWidth)
    val bytes = new Array[Int](colors.length * 3)
    for (i ← colors.indices) {
      val color = new Color(colors(i))
      bytes(i * 3) = color.getRed
      bytes(i * 3 + 1) = color.getGreen
      bytes(i * 3 + 2) = color.getBlue
    }
    bytes
  }

  /**
    * Converts RGB array to color array, which can be used in [[java.awt.image.BufferedImage#setRGB(int, int, int, int, int[], int, int) setRGB function]].
    * Input format: {{{Array(red, green, blue)}}}
    * Output format: {{{Array(rgb)}}}
    * @param arr RGB array
    * @return Color array
    */
  @inline
  private def asRgbColors(arr: Array[Int]): Array[Int] = {
    val result = new Array[Int](arr.length / 3)
    for (i ← result.indices) {
      result(i) = new Color(arr(i * 3), arr(i * 3 + 1), arr(i * 3 + 2)).getRGB
    }
    result
  }

  /**
    * Encodes provided data to RGB bytes
    * @param bytes RGB array
    * @param data Payload
    * @param byteIndex Data offset
    */
  private def writeBytes(bytes: Array[Int], data: ByteString, byteIndex: Int): Unit = {
    val bitOffset = byteIndex * 8
    val bitSet = util.BitSet.valueOf(data.toArray)
    val bitCount = data.length * 8
    for (i ← 0 until bitCount) {
      val index: Int = bitOffset + i
      val evenByte = bytes(index) - (bytes(index) % 2)
      bytes(index) = evenByte + (if (bitSet.get(i)) 1 else 0)
    }
  }

  override def encode(data: ByteString): ByteString = {
    // Request source image for provided data
    val img = sourceImage(data)
    assert(img.ne(null), "Container image not found")

    // Decode RGB data
    val bytes: Array[Int] = asRgbBytes(img)
    val requiredSize: Int = PngEncoding.requiredSize(data)
    assert(bytes.length >= requiredSize, s"Image is too small, $requiredSize bits required")

    // Write length
    writeBytes(bytes, asBytes(data.length), 0)

    // Write payload
    writeBytes(bytes, data, 4)

    // Convert bytes to RGB data
    val rgb = asRgbColors(bytes)
    img.setRGB(0, 0, img.getWidth, img.getHeight, rgb, 0, img.getWidth)

    // Render image as PNG
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(img, "png", outputStream)
    val result = ByteString(outputStream.toByteArray)
    outputStream.close()
    result
  }

  override def decode(data: ByteString): ByteString = {
    // Decode image
    val inputStream = new ByteArrayInputStream(data.toArray)
    val img = ImageIO.read(inputStream)
    inputStream.close()
    assert(img.ne(null), "Invalid image")

    // Decode RGB data
    val bytes: Array[Int] = asRgbBytes(img)

    // Read data length
    val length: Int = asInt(readBytes(bytes, 4, 0))

    // Read payload
    readBytes(bytes, length, 4)
  }
}
