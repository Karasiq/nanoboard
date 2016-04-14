package com.karasiq.nanoboard.server

import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream._
import akka.stream.scaladsl._
import com.karasiq.nanoboard.dispatcher.NanoboardSlickDispatcher
import com.karasiq.nanoboard.model.{Place, _}
import com.karasiq.nanoboard.server.utils.{CaptchaLoader, MessageValidator}
import com.karasiq.nanoboard.sources.bitmessage.BitMessageTransport
import com.karasiq.nanoboard.sources.png.UrlPngSource
import com.karasiq.nanoboard.streaming.NanoboardEvent
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardLegacy, NanoboardMessage}
import com.typesafe.config.ConfigFactory
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Main extends App {
  // Initialize configuration
  val config = {
    val default = ConfigFactory.load()
    val external = Paths.get(default.getString("nanoboard.external-config-file"))
    if (Files.isRegularFile(external)) {
      ConfigFactory.parseFile(external.toFile)
        .withFallback(default)
        .resolve()
    } else {
      default
    }
  }

  implicit val actorSystem = ActorSystem("nanoboard-server", config)
  implicit val executionContext = actorSystem.dispatcher
  implicit val actorMaterializer = ActorMaterializer(ActorMaterializerSettings(actorSystem))
  val db = Database.forConfig("nanoboard.database", config)

  actorSystem.registerOnTermination(db.close())

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = {
      actorSystem.log.info("Shutting down nanoboard-server")
      Await.result(actorSystem.terminate(), Duration.Inf)
    }
  }))

  // Initialize database
  def createSchema = DBIO.seq(
    containers.schema.create,
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

  // Initialize server
  actorSystem.log.info("Loading captcha file")
  schema.flatMap(_ ⇒ CaptchaLoader.load(config)).foreach { captcha ⇒
    actorSystem.registerOnTermination(captcha.close())
    actorSystem.log.info("Captcha file loaded successfully ({} entries)", captcha.length)
    val messageValidator = MessageValidator(captcha, config)

    // Initialize transport
    val bitMessage = BitMessageTransport(config)
    val dispatcher = NanoboardSlickDispatcher(db, captcha, config, Sink.foreach { event ⇒
      actorSystem.eventStream.publish(event)

      event match {
        case NanoboardEvent.PostAdded(message, true) ⇒
          bitMessage.sendMessage(MessageConversions.unwrapToMessage(message)).foreach { response ⇒
            actorSystem.log.info("Message was sent to BM transport: {}", response)
          }

        case _ ⇒
        // Pass
      }
    })

    // Bitmessage
    if (config.getBoolean("nanoboard.bitmessage.receive")) {
      val host = config.getString("nanoboard.bitmessage.listen-host")
      val port = config.getInt("nanoboard.bitmessage.listen-port")
      bitMessage.receiveMessages(host, port, Sink.foreach { (message: NanoboardMessage) ⇒
        messageValidator.isMessageValid(message)
          .filter(identity)
          .foreach(_ ⇒ dispatcher.addPost(s"bitmessage://${LocalDate.now()}", message))
      })
    }

    // Imageboards PNG
    val messageSource = UrlPngSource(config)
    val updateInterval = FiniteDuration(config.getDuration("nanoboard.scheduler.update-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    val maxNewPosts = config.getInt("nanoboard.scheduler.posts-per-container")
    val placeFlow = Flow[String]
      .named("board-png-flow")
      .flatMapMerge(4, messageSource.imagesFromPage)
      .mapAsync(1)(url ⇒ db.run(for (e ← containers.filter(_.url === url).exists.result) yield (e, url)))
      .filterNot(_._1)
      .map(_._2)
      .log("board-png-source")
      .flatMapMerge(4, url ⇒ messageSource.messagesFromImage(url).fold(Vector.empty[NanoboardMessage])(_ :+ _).map((url, _)))
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.restartingDecider) and
        Attributes.logLevels(Logging.InfoLevel, onFailure = Logging.WarningLevel))

    Source.tick(10 seconds, updateInterval, akka.NotUsed)
      .flatMapConcat(_ ⇒ Source.fromPublisher(db.stream(Place.list())))
      .via(placeFlow)
      .runForeach {
        case (url, messages) ⇒
          def insertMessages(messages: Seq[NanoboardMessage], inserted: Int = 0): Unit = messages match {
            case Seq(message, ms @ _*) if inserted < maxNewPosts ⇒
              messageValidator.isMessageValid(message)
                .flatMap(valid ⇒ if (valid) dispatcher.addPost(url, message) else Future.successful(0))
                .foreach(i ⇒ insertMessages(ms, inserted + i))

            case Seq(message) if inserted < maxNewPosts ⇒
              messageValidator.isMessageValid(message)
                .filter(identity)
                .foreach(_ ⇒ dispatcher.addPost(url, message))

            case _ ⇒
              ()
          }

          db.run(Container.create(url)).foreach { _ ⇒
            insertMessages(messages)
          }
      }

    // REST server
    val server = NanoboardServer(dispatcher)
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
