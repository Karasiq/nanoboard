package com.karasiq.nanoboard.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import boopickle.Default._
import com.karasiq.nanoboard.dispatcher.NanoboardDispatcher
import com.karasiq.nanoboard.server.util.AttachmentGenerator
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private[server] case class NanoboardReply(parent: String, message: String)

private[server] final class NanoboardServer(dispatcher: NanoboardDispatcher)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends BinaryMarshaller {
  private implicit def ec: ExecutionContext = actorSystem.dispatcher

  private val maxPostSize = actorSystem.settings.config.getMemorySize("nanoboard.max-post-size").toBytes

  val route = {
    get {
      encodeResponse {
        path("post" / NanoboardMessage.hashRegex) { hash ⇒
          complete(StatusCodes.OK, dispatcher.post(hash))
        } ~
        (pathPrefix("posts") & parameters('offset.as[Int].?(0), 'count.as[Int].?(100))) { (offset, count) ⇒
          path(NanoboardMessage.hashRegex) { hash ⇒
            complete(StatusCodes.OK, dispatcher.thread(hash, offset, count))
          } ~
          pathEndOrSingleSlash {
            complete(StatusCodes.OK, dispatcher.recent(offset, count))
          }
        } ~
        (path("pending") & parameters('offset.as[Int].?(0), 'count.as[Int].?(100))) { (offset, count) ⇒
          complete(StatusCodes.OK, dispatcher.pending(offset, count))
        } ~
        path("categories") {
          complete(StatusCodes.OK, dispatcher.categories())
        } ~
        path("places") {
          complete(StatusCodes.OK, dispatcher.places())
        } ~
        pathEndOrSingleSlash {
          getFromResource("webapp/index.html")
        } ~
        getFromResourceDirectory("webapp")
      }
    } ~
    post {
      (path("post") & entity(as[NanoboardReply])) { case NanoboardReply(parent, message) ⇒
        if (message.length <= maxPostSize) {
          complete(StatusCodes.OK, dispatcher.reply(parent, message))
        } else {
          complete(StatusCodes.custom(400, s"Message is too long. Max size is $maxPostSize bytes"), HttpEntity(""))
        }
      } ~
      (path("container") & parameters('pending.as[Int].?(10), 'random.as[Int].?(50), 'format.?("png")) & entity(as[ByteString]) & extractLog) { (pending, random, format, entity, log) ⇒
        onComplete(dispatcher.createContainer(pending, random, format, entity)) {
          case Success(data) ⇒
            complete(StatusCodes.OK, HttpEntity(data))

          case Failure(exc) ⇒
            log.error(exc, "Container creation error")
            complete(StatusCodes.custom(500, "Container creation error"), HttpEntity(ByteString.empty))
        }
      } ~
      (path("attachment") & parameters('format.?("jpeg"), 'size.as[Int].?(500), 'quality.as[Int].?(70)) & entity(as[ByteString])) { (format, size, quality, data) ⇒
        complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, AttachmentGenerator.createImage(format, size, quality, data)))
      }
    } ~
    delete {
      path("post" / NanoboardMessage.hashRegex) { hash ⇒
        extractLog { log ⇒
          log.info("Post permanently deleted: {}", hash)
          complete(StatusCodes.OK, dispatcher.delete(hash))
        }
      }
    } ~
    put {
      (path("places") & entity(as[Seq[String]]) & extractLog) { (places, log) ⇒
        log.info("Places updated: {}", places)
        complete(StatusCodes.OK, dispatcher.updatePlaces(places))
      } ~
      (path("categories") & entity(as[Seq[NanoboardCategory]]) & extractLog) { (categories, log) ⇒
        log.info("Categories updated: {}", categories)
        complete(StatusCodes.OK, dispatcher.updateCategories(categories))
      }
    }
  }
}
