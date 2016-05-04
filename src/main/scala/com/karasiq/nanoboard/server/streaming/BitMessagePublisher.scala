package com.karasiq.nanoboard.server.streaming

import akka.actor.{Actor, ActorLogging, Props}
import com.karasiq.nanoboard.model.MessageConversions
import com.karasiq.nanoboard.sources.bitmessage.BitMessageTransport
import com.karasiq.nanoboard.streaming.NanoboardEvent

import scala.util.{Failure, Success}

private[server] object BitMessagePublisher {
  def props(bitMessage: BitMessageTransport) = {
    Props(classOf[BitMessagePublisher], bitMessage)
  }
}

private[server] class BitMessagePublisher(bitMessage: BitMessageTransport) extends Actor with ActorLogging {
  import context.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[NanoboardEvent])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
    super.postStop()
  }

  override def receive: Receive = {
    case NanoboardEvent.PostVerified(message) ⇒
      log.debug("Sending message to BM transport: {}", message)
      bitMessage.sendMessage(MessageConversions.unwrapToMessage(message)).onComplete {
        case Success(response) ⇒
          log.info("Message was sent to BM transport: {}", response)

        case Failure(exc) ⇒
          if (log.isDebugEnabled) {
            log.error(exc, "Error sending message to BM transport: {}", message)
          }
      }
  }
}
