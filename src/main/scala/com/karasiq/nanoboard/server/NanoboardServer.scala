package com.karasiq.nanoboard.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.karasiq.nanoboard.NanoboardCategory
import com.karasiq.nanoboard.dispatcher.NanoboardDispatcher

import scala.concurrent.ExecutionContext

case class NanoboardReply(parent: String, message: String)


final class NanoboardServer(dispatcher: NanoboardDispatcher)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends UpickleMarshaller {
  private implicit def ec: ExecutionContext = actorSystem.dispatcher

  private val sha256HashRegex = "[A-Za-z0-9]{32}".r

  val route = {
    get {
      encodeResponse {
        (path("posts" / sha256HashRegex) & parameters('offset.as[Int].?(0), 'count.as[Int].?(100))) { (hash, offset, count) ⇒
          complete(StatusCodes.OK, dispatcher.get(hash, offset, count))
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
      (path("post") & entity[NanoboardReply](jsonUnmarshaller)) { case NanoboardReply(parent, message) ⇒
        complete(StatusCodes.OK, dispatcher.reply(parent, message))
      }
    } ~
    delete {
      path("post" / sha256HashRegex) { hash ⇒
        extractLog { log ⇒
          log.info("Post permanently deleted: {}", hash)
          complete(StatusCodes.OK, dispatcher.delete(hash))
        }
      }
    } ~
    put {
      (path("places") & entity[Seq[String]](jsonUnmarshaller) & extractLog) { (places, log) ⇒
        log.info("Places updated: {}", places)
        complete(StatusCodes.OK, dispatcher.updatePlaces(places))
      } ~
      (path("categories") & entity[Seq[NanoboardCategory]](jsonUnmarshaller) & extractLog) { (categories, log) ⇒
        log.info("Categories updated: {}", categories)
        complete(StatusCodes.OK, dispatcher.updateCategories(categories))
      }
    }
  }
}
