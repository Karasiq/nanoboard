package com.karasiq.nanoboard.server.streaming

import akka.actor.Props
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import boopickle.Default._
import com.karasiq.nanoboard.streaming.NanoboardSubscription.{PostHashes, Unfiltered}
import com.karasiq.nanoboard.streaming.{NanoboardEvent, NanoboardSubscription}

private[server] final class NanoboardMessageStream extends GraphStage[FanInShape2[NanoboardSubscription, NanoboardEvent, NanoboardEvent]] {
  val input: Inlet[NanoboardSubscription] = Inlet("SubscriptionInput")
  val events: Inlet[NanoboardEvent] = Inlet("EventStream")
  val output: Outlet[NanoboardEvent] = Outlet("EventOutput")

  override def shape = new FanInShape2(input, events, output)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    private var subscription: NanoboardSubscription = PostHashes(Set.empty)

    def request(): Unit = {
      if (!hasBeenPulled(events)) {
        pull(events)
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

    setHandler(events, new InHandler {
      override def onPush(): Unit = {
        grab(events) match {
          case added @ NanoboardEvent.PostAdded(message) ⇒
            subscription match {
              case PostHashes(hashes) ⇒
                if (hashes.exists(message.parent.contains) || hashes.contains(message.hash)) {
                  emit(output, added)
                }

              case Unfiltered ⇒
                emit(output, added)
            }

          case event ⇒
            emit(output, event)
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

private[server] object NanoboardMessageStream {
  import com.karasiq.nanoboard.streaming.NanoboardEvent._
  import com.karasiq.nanoboard.streaming.NanoboardSubscription._

  def flow = Flow.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[akka.NotUsed] ⇒
    import GraphDSL.Implicits._
    val in = b.add {
      Flow[Message]
        .named("websocket-input")
        .flatMapConcat {
          case bm: BinaryMessage ⇒
            bm.dataStream.fold(ByteString.empty)(_ ++ _)

          case tm: TextMessage ⇒
            tm.textStream.fold("")(_ ++ _).map(ByteString(_))
        }
        .map(bs ⇒ Unpickle[NanoboardSubscription].fromBytes(bs.toByteBuffer))
    }

    val out = b.add {
      Flow[NanoboardEvent]
        .named("websocket-output")
        .map(event ⇒ BinaryMessage(ByteString(Pickle.intoBytes(event))))
    }

    val messages = b.add(Source.actorPublisher[NanoboardEvent](Props[NanoboardMessagePublisher]))

    val processor = b.add(new NanoboardMessageStream)

    in.out ~> processor.in0
    messages.out ~> processor.in1
    processor.out ~> out.in
    FlowShape(in.in, out.out)
  })
}