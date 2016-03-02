package com.karasiq.nanoboard.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.karasiq.nanoboard.dispatcher.NanoboardDispatcher
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.concurrent.ExecutionContext

case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

final class NanoboardServer(dispatcher: NanoboardDispatcher)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends UpickleMarshaller {
  private implicit def ec: ExecutionContext = actorSystem.dispatcher

  private val sha256HashRegex = "[A-Za-z0-9]{32}".r

  val route = {
    get {
      encodeResponse {
        pathPrefix("posts") {
          path(sha256HashRegex) { hash ⇒
            val result = dispatcher.get(hash).map(_.map {
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
    }
  }
}
