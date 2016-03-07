package com.karasiq.nanoboard.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.karasiq.nanoboard.dispatcher.NanoboardSlickDispatcher
import com.karasiq.nanoboard.server.cache.MapDbNanoboardCache
import com.karasiq.nanoboard.server.model.{Place, Post, _}
import com.karasiq.nanoboard.sources.png.UrlPngSource
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardLegacy}
import com.typesafe.config.ConfigFactory
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


object Main extends App {
  val config = ConfigFactory.load()
  implicit val actorSystem = ActorSystem("nanoboard-server", config)
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer()
  val db = Database.forConfig("nanoboard.database")
  val dispatcher = new NanoboardSlickDispatcher(config, db)
  val cache = new MapDbNanoboardCache(config.getConfig("nanoboard.scheduler.cache"))
  val maxPostSize = config.getMemorySize("nanoboard.max-post-size").toBytes

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
    categories += NanoboardCategory("bdd4b5fc1b3a933367bc6830fef72a35", "Metacategory"),
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
    val messageSource = UrlPngSource()
    val spamFilter = config.getStringList("nanoboard.scheduler.spam-filter").toVector

    Source.tick(10 seconds, FiniteDuration(actorSystem.settings.config.getDuration("nanoboard.scheduler.update-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS), ())
      .flatMapConcat(_ ⇒ Source.fromPublisher(db.stream(Place.list())))
      .flatMapMerge(8, messageSource.imagesFromPage)
      .filterNot(cache.contains)
      .alsoTo(Sink.foreach(image ⇒ cache += image))
      .flatMapMerge(8, messageSource.messagesFromImage)
      .filter(message ⇒ message.text.length <= maxPostSize && spamFilter.forall(!message.text.matches(_)))
      .runWith(Sink.foreach(message ⇒ db.run(Post.insertMessage(message))))

    val server = new NanoboardServer(dispatcher)
    val host = config.getString("nanoboard.server.host")
    val port = config.getInt("nanoboard.server.port")
    Http().bindAndHandle(server.route, host, port).onComplete {
      case Success(ServerBinding(address)) ⇒
        actorSystem.log.info("Nanoboard server listening at {}", address)

      case Failure(exc) ⇒
        actorSystem.log.error(exc, "Port binding failure")
    }
  }
}
