package com.karasiq.nanoboard.server

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import com.karasiq.nanoboard.NanoboardMessage

import scala.annotation.tailrec

private[server] class NanoboardMessagePublisher extends ActorPublisher[NanoboardMessage] {
  override def preStart(): Unit = {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[NanoboardMessage])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
    super.postStop()
  }

  val maxBufferSize = 20
  var messageBuffer = Vector.empty[NanoboardMessage]

  override def receive: Receive = {
    case m: NanoboardMessage ⇒
      if (messageBuffer.isEmpty && totalDemand > 0) {
        onNext(m)
      } else {
        if (messageBuffer.length >= maxBufferSize) {
          messageBuffer = messageBuffer.tail :+ m
        } else {
          messageBuffer :+= m
        }
        deliverBuffer()
      }

    case Request(_) ⇒
      deliverBuffer()

    case Cancel ⇒
      context.stop(self)
  }

  @tailrec final def deliverBuffer(): Unit = {
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = messageBuffer.splitAt(totalDemand.toInt)
        messageBuffer = keep
        use foreach onNext
      } else {
        val (use, keep) = messageBuffer.splitAt(Int.MaxValue)
        messageBuffer = keep
        use foreach onNext
        deliverBuffer()
      }
    }
  }
}
