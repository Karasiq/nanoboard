package com.karasiq.nanoboard.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.karasiq.nanoboard.dispatcher.NanoboardDispatcher
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.concurrent.ExecutionContext

case class NanoboardReply(parent: String, message: String)
case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

final class NanoboardServer(dispatcher: NanoboardDispatcher)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends UpickleMarshaller {
  private implicit def ec: ExecutionContext = actorSystem.dispatcher

  private val sha256HashRegex = "[A-Za-z0-9]{32}".r

  val route = {
    get {
      encodeResponse {
        pathPrefix("posts") {
          (path(sha256HashRegex) & parameters('offset.as[Int].?(0), 'count.as[Int].?(100))) { (hash, offset, count) ⇒
            val result = dispatcher.get(hash, offset, count).map(_.map {
              case (m @ NanoboardMessage(parent, text), answers) ⇒
                NanoboardMessageData(Some(parent), m.hash, text, answers)
            })
            complete(StatusCodes.OK, result)
          } ~
          pathEndOrSingleSlash {
            val result = dispatcher.categories().map(_.map {
              case (NanoboardCategory(hash, name), answers) ⇒
                NanoboardMessageData(None, hash, name, answers)
            })
            complete(StatusCodes.OK, result)
          }
        } ~
        pathEndOrSingleSlash {
          getFromResource("webapp/index.html")
        } ~
        getFromResourceDirectory("webapp")
      }
    } ~
    post {
      (path("post") & entity[NanoboardReply](jsonUnmarshaller)) { case NanoboardReply(parent, message) ⇒
        complete(StatusCodes.OK, dispatcher.reply(parent, message).map { m ⇒
          NanoboardMessageData(Some(m.parent), m.hash, m.text, 0)
        })
      }
    } ~
    delete {
      path("post" / sha256HashRegex) { hash ⇒
        complete(StatusCodes.OK, dispatcher.delete(hash))
      }
    } ~
    put {
      (path("places") & entity[Seq[String]](jsonUnmarshaller)) { places ⇒
        complete(StatusCodes.OK, dispatcher.updatePlaces(places))
      } ~
      (path("categories") & entity[Seq[NanoboardCategory]](jsonUnmarshaller)) { categories ⇒
        complete(StatusCodes.OK, dispatcher.updateCategories(categories))
      }
    }
  }
}
