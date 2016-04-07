package com.karasiq.nanoboard.encoding

import com.karasiq.nanoboard.encoding.DataEncodingStage._
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.typesafe.config.{Config, ConfigFactory}

object NanoboardEncoding {
  /**
    * Creates default nanoboard data encoder with specified config
    * @param config Configuration object
    * @param pngEncoding PNG encoder
    * @return Default nanoboard data encoder
    * @see [[https://github.com/nanoboard/nanoboard/wiki/%D0%9D%D0%B0%D0%BD%D0%BE%D0%B1%D0%BE%D1%80%D0%B4%D0%B0 Original specification (Russian)]]
    */
  def fromConfig(config: Config, pngEncoding: PngEncoding = PngEncoding.decoder): DataEncodingStage = {
    Seq(GzipCompression(), SalsaCipher.fromConfig(config), pngEncoding)
  }

  /**
    * Same as [[com.karasiq.nanoboard.encoding.NanoboardEncoding#fromConfig(com.typesafe.config.Config, com.karasiq.nanoboard.encoding.stages.PngEncoding) fromConfig]],
    * but uses root configuration.
    */
  def apply(config: Config = ConfigFactory.load(), pngEncoding: PngEncoding = PngEncoding.decoder): DataEncodingStage = {
    fromConfig(config.getConfig("nanoboard"))
  }
}
