package com.karasiq.nanoboard.server.util

import java.awt.RenderingHints._
import java.awt.image._
import java.awt.{Dimension, Image, Toolkit}
import java.io.ByteArrayOutputStream
import javax.imageio.stream.ImageOutputStream
import javax.imageio.{IIOImage, ImageIO, ImageWriteParam, ImageWriter}

import akka.util.ByteString
import org.apache.commons.codec.binary.Base64

import scala.collection.JavaConversions._

object AttachmentGenerator {
  private val resizer = new ImageResizingUtil

  def createImage(format: String, size: Int, quality: Int, data: ByteString): ByteString = {
    ByteString(Base64.encodeBase64(resizer.compress(resizer.resize(data.toArray, size), format, quality)))
  }
}

private[util] final class ImageResizingUtil {
  private def getScaledDimension(imgSize: Dimension, boundary: Dimension): Dimension = {
    var newWidth: Int = imgSize.width
    var newHeight: Int = imgSize.height
    if (imgSize.width > boundary.width) {
      newWidth = boundary.width
      newHeight = (newWidth * imgSize.height) / imgSize.width
    }
    if (newHeight > boundary.height) {
      newHeight = boundary.height
      newWidth = (newHeight * imgSize.width) / imgSize.height
    }
    new Dimension(newWidth, newHeight)
  }

  private def redrawImage(img: Image, size: Dimension): BufferedImage = {
    val bufferedImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB)
    val graphics = bufferedImage.createGraphics()
    try {
      graphics.setRenderingHints(Map(
        KEY_RENDERING → VALUE_RENDER_QUALITY,
        KEY_COLOR_RENDERING → VALUE_COLOR_RENDER_QUALITY,
        KEY_ANTIALIASING → VALUE_ANTIALIAS_ON,
        KEY_INTERPOLATION → VALUE_INTERPOLATION_BICUBIC
      ))
      graphics.drawImage(img, 0, 0, size.width, size.height, null)
      bufferedImage
    } finally {
      graphics.dispose()
    }
  }

  // JPG colors fix
  private def loadImage(image: Array[Byte]): BufferedImage = {
    val img = Toolkit.getDefaultToolkit.createImage(image)

    val RGB_MASKS: Array[Int] = Array(0xFF0000, 0xFF00, 0xFF)
    val RGB_OPAQUE: ColorModel = new DirectColorModel(32, RGB_MASKS(0), RGB_MASKS(1), RGB_MASKS(2))

    val pg = new PixelGrabber(img, 0, 0, -1, -1, true)
    pg.grabPixels()

    (pg.getWidth, pg.getHeight, pg.getPixels) match {
      case (width, height, pixels: Array[Int]) ⇒
        val buffer = new DataBufferInt(pixels, width * height)
        val raster = Raster.createPackedRaster(buffer, width, height, width, RGB_MASKS, null)
        new BufferedImage(RGB_OPAQUE, raster, false, null)

      case _ ⇒
        throw new IllegalArgumentException("Invalid image")
    }
  }

  def compress(image: BufferedImage, format: String, quality: Int): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    val ios: ImageOutputStream = ImageIO.createImageOutputStream(outputStream)
    try {
      val writer: ImageWriter = ImageIO.getImageWritersByFormatName(format).next
      val iwp: ImageWriteParam = writer.getDefaultWriteParam
      iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
      iwp.setCompressionQuality(quality.toFloat / 100.0f)
      writer.setOutput(ios)
      writer.write(null, new IIOImage(image, null, null), iwp)
      writer.dispose()
      ios.flush()
      outputStream.toByteArray
    } finally {
      ios.close()
      outputStream.close()
    }
  }

  def resize(data: Array[Byte], size: Int): BufferedImage = {
    val image = loadImage(data)
    val imgSize = new Dimension(image.getWidth, image.getHeight)
    val boundary = new Dimension(size, size)
    val newSize = getScaledDimension(imgSize, boundary)
    redrawImage(image, newSize)
  }
}
