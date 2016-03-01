package com.karasiq.nanoboard.server

import akka.actor.ActorSystem
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration._


object Main extends App {
  val actorSystem = ActorSystem("nanoboard-server")
  val db = Database.forConfig("nanoboard.database")
  actorSystem.registerOnTermination(db.close())

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = {
      actorSystem.log.info("Shutting down nanoboard-server")
      Await.result(actorSystem.terminate(), Duration.Inf)
    }
  }))


}
