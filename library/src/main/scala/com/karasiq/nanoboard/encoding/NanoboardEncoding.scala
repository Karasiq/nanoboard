package com.karasiq.nanoboard.encoding

import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.typesafe.config.{Config, ConfigFactory}

object NanoboardEncoding {
  def apply(config: Config, pngEncoding: PngEncoding = PngEncoding.decoder): DataEncodingStage = {
    Seq(GzipCompression(), SalsaCipher(config), pngEncoding)
  }

  def apply(): DataEncodingStage = {
    apply(ConfigFactory.load())
  }
}
