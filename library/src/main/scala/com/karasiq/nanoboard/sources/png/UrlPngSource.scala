package com.karasiq.nanoboard.sources.png

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.encoding.{DataEncodingStage, NanoboardEncoding}

/**
  * PNG downloader interface
  */
trait UrlPngSource {
  /**
    * Downloads and parses the messages from provided URL
    * @param url PNG image URL
    * @return Nanoboard messages
    */
  def messagesFromImage(url: String): Source[NanoboardMessage, akka.NotUsed]

  /**
    * Downloads and provides list of available PNG images from the page
    * @param url Page URL
    * @return PNG images URL
    */
  def imagesFromPage(url: String): Source[String, akka.NotUsed]
}

object UrlPngSource {
  def fromConfig(config: Config)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    apply(NanoboardEncoding.fromConfig(config))
  }

  def apply(config: Config)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    apply(NanoboardEncoding(config))
  }

  def apply(encoding: DataEncodingStage)(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    new BoardPngSource(encoding)
  }

  def apply()(implicit as: ActorSystem, am: ActorMaterializer): UrlPngSource = {
    apply(as.settings.config)
  }
}