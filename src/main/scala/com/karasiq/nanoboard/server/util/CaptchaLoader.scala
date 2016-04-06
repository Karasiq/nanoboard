package com.karasiq.nanoboard.server.util

import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import com.karasiq.nanoboard.captcha.NanoboardCaptchaFile
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContext, Future}

object CaptchaLoader {
  def apply(config: Config = ConfigFactory.load())(implicit am: ActorMaterializer, as: ActorSystem, ec: ExecutionContext): CaptchaLoader = {
    new CaptchaLoader(Paths.get(config.getString("nanoboard.captcha.storage")))
  }

  def load(config: Config = ConfigFactory.load())(implicit am: ActorMaterializer, as: ActorSystem, ec: ExecutionContext): Future[NanoboardCaptchaFile] = {
    apply(config).forUrl(config.getString("nanoboard.captcha.download-url"))
  }
}

final class CaptchaLoader(baseDir: Path)(implicit am: ActorMaterializer, as: ActorSystem, ec: ExecutionContext) {
  private val http = Http()

  // TODO: https://github.com/akka/akka/issues/15990
  private def requestWithRedirects(uri: Uri): Future[HttpResponse] = {
    http.singleRequest(HttpRequest(uri = uri)).flatMap { response ⇒
      val location = response.header[Location]
      if (response.status.isRedirection() && location.isDefined) {
        requestWithRedirects(location.get.uri)
      } else {
        Future.successful(response)
      }
    }
  }

  def forUrl(url: String): Future[NanoboardCaptchaFile] = {
    val fileName = baseDir.resolve(s"${Integer.toHexString(url.hashCode)}.nbc")
    if (Files.exists(fileName)) {
      assert(Files.isRegularFile(fileName), s"Not a file: $fileName")
      Future.successful(NanoboardCaptchaFile(fileName.toString))
    } else {
      Source
        .fromFuture(requestWithRedirects(url))
        .flatMapConcat(_.entity.dataBytes)
        .toMat(FileIO.toFile(fileName.toFile))((_, r) ⇒ r.map { ioResult ⇒
          if (ioResult.wasSuccessful) NanoboardCaptchaFile(fileName.toString)
          else throw ioResult.getError
        })
        .run()
    }
  }
}
