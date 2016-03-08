package com.karasiq.nanoboard.sources.png

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.DataEncodingStage
import com.karasiq.nanoboard.encoding.stages.{GzipCompression, PngEncoding, SalsaCipher}
import com.typesafe.config.{Config, ConfigFactory}

trait UrlPngSource {
  def messagesFromImage(url: String): Source[NanoboardMessage, akka.NotUsed]
  def imagesFromPage(url: String): Source[String, akka.NotUsed]
}

object UrlPngSource {
  def fromConfig(config: Config)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    new DefaultUrlPngSource(Seq(GzipCompression(), SalsaCipher.fromConfig(config), PngEncoding.decoder))
  }

  def apply(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    new DefaultUrlPngSource(encoding)
  }

  def apply()(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    fromConfig(ConfigFactory.load())
  }
}