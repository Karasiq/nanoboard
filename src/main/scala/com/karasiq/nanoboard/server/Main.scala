package com.karasiq.nanoboard.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.karasiq.nanoboard.dispatcher.NanoboardSlickDispatcher
import com.karasiq.nanoboard.server.cache.MapDbNanoboardCache
import com.karasiq.nanoboard.server.model.{Place, Post}
import com.karasiq.nanoboard.sources.BoardPngSource
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object Main extends App {
  implicit val actorSystem = ActorSystem("nanoboard-server")
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer()
  val db = Database.forConfig("nanoboard.database")
  val dispatcher = new NanoboardSlickDispatcher(db)
  val cache = new MapDbNanoboardCache(actorSystem.settings.config.getConfig("nanoboard.scheduler.cache"))
  actorSystem.registerOnTermination(db.close())

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = {
      actorSystem.log.info("Shutting down nanoboard-server")
      Await.result(actorSystem.terminate(), Duration.Inf)
    }
  }))

  val messageSource = BoardPngSource()

  Source.tick(5 seconds, FiniteDuration(actorSystem.settings.config.getDuration("nanoboard.scheduler.update-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS), ())
    .flatMapConcat(_ ⇒ Source.fromPublisher(db.stream(Place.list())))
    .flatMapConcat(messageSource.imagesFromPage)
    .filterNot(cache.contains)
    .alsoTo(Sink.foreach(image ⇒ cache += image))
    .flatMapConcat(messageSource.messagesFromImage)
    .mapAsyncUnordered(10)(message ⇒ db.run(Post.insertMessage(message)))
    .runWith(Sink.ignore)

  // TODO: Server
}
