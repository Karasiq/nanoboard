package com.karasiq.nanoboard.server

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import boopickle.Default._
import com.karasiq.nanoboard.NanoboardMessage

private[server] final class NanoboardMessageStream extends GraphStage[FanInShape2[Set[String], NanoboardMessage, NanoboardMessage]] {
  val input: Inlet[Set[String]] = Inlet("SubscriptionInput")
  val messages: Inlet[NanoboardMessage] = Inlet("MessageStream")
  val output: Outlet[NanoboardMessage] = Outlet("MessageOutput")

  override def shape = new FanInShape2(input, messages, output)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    private var subscription = Set.empty[String]

    def request(): Unit = {
      if (!hasBeenPulled(messages)) {
        pull(messages)
      }
      if (!hasBeenPulled(input)) {
        pull(input)
      }
    }

    setHandler(input, new InHandler {
      override def onPush(): Unit = {
        subscription = grab(input)
        request()
      }
    })

    setHandler(messages, new InHandler {
      override def onPush(): Unit = {
        val message = grab(messages)
        if (subscription.contains(message.parent) || subscription.contains(message.hash)) {
          emit(output, message)
        }
        request()
      }
    })

    setHandler(output, new OutHandler {
      override def onPull(): Unit = {
        request()
      }
    })
  }
}
