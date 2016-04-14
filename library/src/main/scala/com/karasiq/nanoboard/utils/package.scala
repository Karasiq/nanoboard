package com.karasiq.nanoboard

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO

import akka.util.ByteString
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.IOUtils

import scala.util.Random

package object utils {
  private[nanoboard] implicit class ByteStringCompanionOps(private val bs: ByteString.type) extends AnyVal {
    /**
      * Creates byte string from hex string (each byte represented as two chars from 00 to FF)
      * @param string Hex string
      * @return Byte string
      * @see [[org.apache.commons.codec.binary.Hex#decodeHex(char[])]]
      */
    def fromHexString(string: String): ByteString = {
      bs.apply(Hex.decodeHex(string.toCharArray))
    }
  }

  private[nanoboard] implicit class ByteStringOps(private val bs: ByteString) extends AnyVal {
    /**
      * Encodes byte string as hex string (each byte represented as two chars from 00 to FF)
      * @param lowerCase Use lower-case hex chars
      * @return Hex string
      * @see [[org.apache.commons.codec.binary.Hex#encodeHex(byte[], boolean)]]
      */
    def toHexString(lowerCase: Boolean = true): String = {
      String.valueOf(Hex.encodeHex(bs.toArray, lowerCase))
    }
  }

  private[nanoboard] object BufferedImages {
    /**
      * Convers color array to RGB array
      * Input format: {{{Array(rgb)}}}
      * Output format: {{{Array(red, green, blue)}}}
      * @param colors Color array
      * @return RGB array
      */
    def toRgbArray(colors: Array[Int]): Array[Int] = {
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
    def toColorArray(arr: Array[Int]): Array[Int] = {
      val result = new Array[Int](arr.length / 3)
      for (i ← result.indices) {
        result(i) = new Color(arr(i * 3), arr(i * 3 + 1), arr(i * 3 + 2)).getRGB
      }
      result
    }

    /**
      * Generates random image
      * @param width Image width
      * @param height Image height
      * @return Generated image
      */
    def generateImage(width: Int, height: Int): BufferedImage = {
      val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val rgb = Array.fill[Int](width * height)(Random.nextInt())
      image.setRGB(0, 0, width, height, rgb, 0, width)
      image
    }

    /**
      * Reads image from bytes
      * @param data Encoded image
      * @return Decoded image
      * @see [[javax.imageio.ImageIO#read(java.io.InputStream)]]
      */
    def fromBytes(data: ByteString): BufferedImage = {
      val inputStream = new ByteArrayInputStream(data.toArray)
      try {
        ImageIO.read(inputStream)
      } finally {
        IOUtils.closeQuietly(inputStream)
      }
    }
  }

  private[nanoboard] implicit class BufferedImageOps(private val img: BufferedImage) extends AnyVal {
    /**
      * Encodes image to color array
      * Output format: {{{Array(rgb)}}}
      * @return Color array
      */
    def toColorArray: Array[Int] = {
      val colors = new Array[Int](img.getWidth * img.getHeight)
      img.getRGB(0, 0, img.getWidth, img.getHeight, colors, 0, img.getWidth)
      colors
    }

    /**
      * Decodes image to RGB array.
      * Output format: {{{Array(red, green, blue)}}}
      * @return RGB array
      */
    def toRgbArray: Array[Int] = {
      BufferedImages.toRgbArray(toColorArray)
    }

    /**
      * Encodes image to bytes
      * @param format Image encoding format
      * @return Encoded image
      * @see [[javax.imageio.ImageIO#write(java.awt.image.RenderedImage, java.lang.String, java.io.OutputStream)]]
      */
    def toBytes(format: String): ByteString = {
      val outputStream = new ByteArrayOutputStream()
      try {
        ImageIO.write(img, format, outputStream)
        ByteString(outputStream.toByteArray)
      } finally {
        IOUtils.closeQuietly(outputStream)
      }
    }
  }
}
