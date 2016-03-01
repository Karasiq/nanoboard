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
  def apply(sourceImage: ByteString ⇒ BufferedImage): PngEncoding = {
    new PngEncoding(sourceImage)
  }

  val decoder = apply(_ ⇒ null)
}

final class PngEncoding(sourceImage: ByteString ⇒ BufferedImage) extends DataEncodingStage {
  implicit val byteOrder = ByteOrder.LITTLE_ENDIAN

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

  private def readBytes(bytes: Array[Int], dataLength: Int, index: Int): ByteString = {
    val bitCount = dataLength * 8
    val offset = index * 8
    val result = new util.BitSet(bitCount)
    for (i ← 0 until bitCount) {
      if (bytes(offset + i) % 2 == 1) result.set(i)
    }
    ByteString(result.toByteArray).take(dataLength)
  }

  @inline
  private def asRgbBytes(img: BufferedImage): Array[Int] = {
    val colors = new Array[Int](img.getWidth * img.getHeight)
    img.getRGB(0, 0, img.getWidth, img.getHeight, colors, 0, img.getWidth)
    val bytes = new Array[Int](colors.length * 4)
    for (i ← colors.indices) {
      val color = new Color(colors(i))
      bytes(i * 3) = color.getRed
      bytes(i * 3 + 1) = color.getGreen
      bytes(i * 3 + 2) = color.getBlue
    }
    bytes
  }

  @inline
  private def asRgbColors(arr: Array[Int]): Array[Int] = {
    val result = new Array[Int](arr.length / 3)
    for (i ← result.indices) {
      result(i) = new Color(arr(i * 3), arr(i * 3 + 1), arr(i * 3 + 2)).getRGB
    }
    result
  }

  override def encode(data: ByteString): ByteString = {
    val img = sourceImage(data)
    assert(img.ne(null), "Container image not found")
    val bytes: Array[Int] = asRgbBytes(img)
    val bitSet = util.BitSet.valueOf((asBytes(data.length) ++ data).toArray)
    val requiredSize: Int = bitSet.length()
    assert(bytes.length >= requiredSize, s"Image is too small, $requiredSize bits required")
    for (i ← bytes.indices.takeWhile(_ <= requiredSize)) {
      val evenByte = bytes(i) - (bytes(i) % 2)
      bytes(i) = evenByte + (if (bitSet.get(i)) 1 else 0)
    }
    val rgb = asRgbColors(bytes)
    img.setRGB(0, 0, img.getWidth, img.getHeight, rgb, 0, img.getWidth)
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(img, "png", outputStream)
    val result = ByteString(outputStream.toByteArray)
    outputStream.close()
    result
  }

  override def decode(data: ByteString): ByteString = {
    val inputStream = new ByteArrayInputStream(data.toArray)
    val img = ImageIO.read(inputStream)
    inputStream.close()
    val bytes: Array[Int] = asRgbBytes(img)
    val length: Int = asInt(readBytes(bytes, 4, 0))
    readBytes(bytes, length, 4)
  }
}
