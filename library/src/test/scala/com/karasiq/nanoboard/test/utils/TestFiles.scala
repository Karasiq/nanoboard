package com.karasiq.nanoboard.test.utils

import java.io.{ByteArrayInputStream, FileOutputStream}
import java.nio.file.Files

import akka.util.ByteString
import org.apache.commons.io.IOUtils

object TestFiles {
  def resource(name: String): ByteString = {
    val input = getClass.getClassLoader.getResourceAsStream("test-encoded.png")
    try {
      ByteString(IOUtils.toByteArray(input))
    } finally {
      IOUtils.closeQuietly(input)
    }
  }

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
