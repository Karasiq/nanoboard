package com.karasiq.nanoboard.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{CacheDirectives, `Cache-Control`}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import boopickle.Default._
import com.karasiq.nanoboard.api.{NanoboardCaptchaAnswer, NanoboardReply}
import com.karasiq.nanoboard.dispatcher.NanoboardDispatcher
import com.karasiq.nanoboard.server.streaming.NanoboardMessageStream
import com.karasiq.nanoboard.server.util.{AttachmentGenerator, FractalMusic}
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object NanoboardServer {
  def apply(dispatcher: NanoboardDispatcher)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): NanoboardServer = {
    new NanoboardServer(dispatcher)
  }
}

private[server] final class NanoboardServer(dispatcher: NanoboardDispatcher)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends BinaryMarshaller {
  private implicit def ec: ExecutionContext = actorSystem.dispatcher

  private val maxPostSize = actorSystem.settings.config.getMemorySize("nanoboard.max-post-size").toBytes

  val route = {
    get {
      path("post" / NanoboardMessage.HASH_FORMAT) { hash ⇒
        complete(StatusCodes.OK, dispatcher.post(hash))
      } ~
      (pathPrefix("posts") & parameters('offset.as[Long].?(0), 'count.as[Long].?(100))) { (offset, count) ⇒
        path(NanoboardMessage.HASH_FORMAT) { hash ⇒
          complete(StatusCodes.OK, dispatcher.thread(hash, offset, count))
        } ~
        pathEndOrSingleSlash {
          complete(StatusCodes.OK, dispatcher.recent(offset, count))
        }
      } ~
      (path("pending") & parameters('offset.as[Long].?(0), 'count.as[Long].?(100))) { (offset, count) ⇒
        complete(StatusCodes.OK, dispatcher.pending(offset, count))
      } ~
      path("categories") {
        complete(StatusCodes.OK, dispatcher.categories())
      } ~
      path("places") {
        complete(StatusCodes.OK, dispatcher.places())
      } ~
      (path("containers") & parameters('offset.as[Long].?(0), 'count.as[Long].?(100))) { (offset, count) ⇒
        complete(StatusCodes.OK, dispatcher.containers(offset, count))
      } ~
      (path("fractal_music" / Segment) & respondWithHeaders(`Cache-Control`(CacheDirectives.public, CacheDirectives.`max-age`(100000000L)))) { formula ⇒
        complete(StatusCodes.OK, FractalMusic(formula).map(HttpEntity(ContentType(MediaType.audio("wav", Compressible)), _)))
      } ~
      (path("verify" / NanoboardMessage.HASH_FORMAT)) { hash ⇒
        complete(StatusCodes.OK, dispatcher.requestVerification(hash))
      } ~
      encodeResponse(pathEndOrSingleSlash(getFromResource("webapp/index.html")) ~ getFromResourceDirectory("webapp"))
    } ~
    post {
      (path("post") & entity(as[NanoboardReply](defaultUnmarshaller))) { case NanoboardReply(parent, message) ⇒
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
      } ~
      (path("verify") & entity[NanoboardCaptchaAnswer](defaultUnmarshaller)) { answer ⇒
        complete(StatusCodes.OK, dispatcher.verifyPost(answer.request, answer.answer))
      }
    } ~
    delete {
      path("post" / NanoboardMessage.HASH_FORMAT) { hash ⇒
        extractLog { log ⇒
          log.info("Post permanently deleted: {}", hash)
          complete(StatusCodes.OK, dispatcher.delete(hash))
        }
      } ~
      (path("posts") & parameter('container.as[Long])) { container ⇒
        complete(StatusCodes.OK, dispatcher.clearContainer(container))
      } ~
      path("pending" / NanoboardMessage.HASH_FORMAT) { hash ⇒
        complete(StatusCodes.OK, dispatcher.markAsNotPending(hash))
      } ~
      (path("posts") & parameters('offset.as[Long].?(0), 'count.as[Long])) { (offset, count) ⇒ // Batch delete
        complete(StatusCodes.OK, dispatcher.delete(offset, count))
      } ~
      path("deleted") {
        complete(StatusCodes.OK, dispatcher.clearDeleted())
      }
    } ~
    put {
      (path("places") & entity(as[Seq[String]](defaultUnmarshaller)) & extractLog) { (places, log) ⇒
        log.info("Places updated: {}", places)
        complete(StatusCodes.OK, dispatcher.updatePlaces(places))
      } ~
      (path("categories") & entity(as[Seq[NanoboardCategory]](defaultUnmarshaller)) & extractLog) { (categories, log) ⇒
        log.info("Categories updated: {}", categories)
        complete(StatusCodes.OK, dispatcher.updateCategories(categories))
      } ~
      path("pending" / NanoboardMessage.HASH_FORMAT) { hash ⇒
        complete(StatusCodes.OK, dispatcher.markAsPending(hash))
      }
    } ~
    path("live") {
      handleWebSocketMessages(NanoboardMessageStream.flow)
    }
  }
}
