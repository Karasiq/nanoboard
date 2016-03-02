package com.karasiq.nanoboard.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.karasiq.nanoboard.NanoboardLegacy
import com.karasiq.nanoboard.dispatcher.NanoboardSlickDispatcher
import com.karasiq.nanoboard.server.cache.MapDbNanoboardCache
import com.karasiq.nanoboard.server.model.{Place, Post, _}
import com.karasiq.nanoboard.sources.BoardPngSource
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


object Main extends App {
  implicit val actorSystem = ActorSystem("nanoboard-server")
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer()
  val db = Database.forConfig("nanoboard.database")
  val dispatcher = new NanoboardSlickDispatcher(db)
  val cache = new MapDbNanoboardCache(actorSystem.settings.config.getConfig("nanoboard.scheduler.cache"))
  actorSystem.registerOnTermination(db.close())
  actorSystem.registerOnTermination(cache.close())

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = {
      actorSystem.log.info("Shutting down nanoboard-server")
      Await.result(actorSystem.terminate(), Duration.Inf)
    }
  }))

  def createSchema = DBIO.seq(
    posts.schema.create,
    deletedPosts.schema.create,
    pendingPosts.schema.create,
    categories.schema.create,
    places.schema.create,
    categories ++= NanoboardLegacy.categoriesFromTxt("categories.txt"),
    places ++= NanoboardLegacy.placesFromTxt("places.txt")
  )

  val schema = Source.fromPublisher(db.stream(MTable.getTables))
    .runWith(Sink.headOption)
    .flatMap {
      case None ⇒
        db.run(createSchema)
      case _ ⇒
        Future.successful(())
    }

  schema.foreach { _ ⇒
    val messageSource = BoardPngSource()

    Source.tick(10 seconds, FiniteDuration(actorSystem.settings.config.getDuration("nanoboard.scheduler.update-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS), ())
      .flatMapConcat(_ ⇒ Source.fromPublisher(db.stream(Place.list())))
      .flatMapMerge(8, messageSource.imagesFromPage)
      .filterNot(cache.contains)
      .alsoTo(Sink.foreach(image ⇒ cache += image))
      .flatMapMerge(8, messageSource.messagesFromImage)
      .runWith(Sink.foreach(message ⇒ db.run(Post.insertMessage(message))))

    val server = new NanoboardServer(dispatcher)
    Http().bindAndHandle(server.route, "127.0.0.1", 7657)
  }
}
