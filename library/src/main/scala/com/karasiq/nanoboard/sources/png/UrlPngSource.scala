package com.karasiq.nanoboard.sources.png

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.{DataEncodingStage, NanoboardEncoding}
import com.typesafe.config.{Config, ConfigFactory}

trait UrlPngSource {
  def messagesFromImage(url: String): Source[NanoboardMessage, akka.NotUsed]
  def imagesFromPage(url: String): Source[String, akka.NotUsed]
}

object UrlPngSource {
  def apply(config: Config)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    new DefaultUrlPngSource(NanoboardEncoding(config))
  }

  def apply(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    new DefaultUrlPngSource(encoding)
  }

  def apply()(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    apply(ConfigFactory.load())
  }
}