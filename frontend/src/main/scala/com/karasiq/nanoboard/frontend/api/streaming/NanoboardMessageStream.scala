package com.karasiq.nanoboard.frontend.api.streaming

import java.nio.ByteBuffer

import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

import boopickle.Default._
import org.scalajs.dom.window
import org.scalajs.dom.raw._

import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.BinaryMarshaller
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.streaming.{NanoboardEvent, NanoboardEventSeq, NanoboardSubscription}
import com.karasiq.nanoboard.streaming.NanoboardSubscription.Unfiltered

object NanoboardMessageStream {
  def apply(f: PartialFunction[NanoboardEvent, Unit])(implicit controller: NanoboardController): NanoboardMessageStream = {
    new NanoboardMessageStream(f)
  }

  private[api] def asArrayBuffer(data: ByteBuffer): ArrayBuffer = {
    if (data.hasTypedArray()) {
      data.typedArray().subarray(data.position, data.limit).asInstanceOf[ArrayBuffer]
    } else {
      val tempBuffer = ByteBuffer.allocateDirect(data.remaining)
      val origPosition = data.position
      tempBuffer.put(data)
      data.position(origPosition)
      tempBuffer.typedArray().asInstanceOf[ArrayBuffer]
    }
  }

  private[api] def asEventSeq(response: Any): NanoboardEventSeq = {
    Unpickle[NanoboardEventSeq].fromBytes(TypedArrayBuffer.wrap(response.asInstanceOf[ArrayBuffer]))
  }
}

// WebSocket wrapper
final class NanoboardMessageStream(f: PartialFunction[NanoboardEvent, Unit])(implicit controller: NanoboardController) {
  private var webSocket: Option[WebSocket] = None
  private var last: NanoboardSubscription = Unfiltered
  private var lastSet = false

  def setContext(context: NanoboardSubscription): Unit = {
    if (!lastSet || context != last) {
      webSocket.foreach { webSocket ⇒
        val buffer = NanoboardMessageStream.asArrayBuffer(BinaryMarshaller.write(context))
        webSocket.send(buffer)
      }
    }
    last = context
    lastSet = webSocket.isDefined
  }

  private def initWebSocket(): Unit = {
    val webSocket = new WebSocket(s"ws://${window.location.host}/live")

    webSocket.binaryType = "arraybuffer"

    webSocket.onmessage = { (m: MessageEvent) ⇒
      val eventSeq = NanoboardMessageStream.asEventSeq(m.data)
      eventSeq.events
        .filter(f.isDefinedAt)
        .foreach(f(_))
    }

    webSocket.onclose = { (e: CloseEvent) ⇒
      this.webSocket = None
      Notifications.warning(s"${controller.locale.webSocketError}: ${e.code} ${e.reason}", Layout.topRight)
      window.setTimeout(() ⇒ initWebSocket(), 3000)
    }

    webSocket.onopen = { (e: Event) ⇒
      this.lastSet = false
      this.webSocket = Some(webSocket)
      setContext(last)
    }
  }

  initWebSocket()
}
