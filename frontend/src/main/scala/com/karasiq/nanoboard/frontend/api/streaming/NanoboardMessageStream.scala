package com.karasiq.nanoboard.frontend.api.streaming

import java.nio.ByteBuffer

import boopickle.Default._
import com.karasiq.nanoboard.frontend.api.BinaryMarshaller
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.streaming.NanoboardSubscription.Unfiltered
import com.karasiq.nanoboard.streaming.{NanoboardEvent, NanoboardSubscription}
import org.scalajs.dom.raw._
import org.scalajs.dom.window

import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object NanoboardMessageStream {
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

  private[api] def asEvent(response: Any): NanoboardEvent = {
    Unpickle[NanoboardEvent].fromBytes(TypedArrayBuffer.wrap(response.asInstanceOf[ArrayBuffer]))
  }

  def apply(f: NanoboardEvent ⇒ Unit): NanoboardMessageStream = {
    new NanoboardMessageStream(f)
  }
}

// WebSocket wrapper
final class NanoboardMessageStream(f: NanoboardEvent ⇒ Unit) {
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
      val message = NanoboardMessageStream.asEvent(m.data)
      f(message)
    }

    webSocket.onclose = { (e: CloseEvent) ⇒
      this.webSocket = None
      Notifications.warning(s"WebSocket was closed: ${e.code} ${e.reason}", Layout.topRight)
      window.setTimeout(initWebSocket _, 3000)
    }

    webSocket.onopen = { (e: Event) ⇒
      this.lastSet = false
      this.webSocket = Some(webSocket)
      setContext(last)
    }
  }

  initWebSocket()
}
