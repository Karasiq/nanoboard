package com.karasiq.nanoboard.frontend.api

import java.nio.ByteBuffer

import boopickle.Default._
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
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

  private[api] def asMessage(response: Any): NanoboardMessageData = {
    Unpickle[NanoboardMessageData].fromBytes(TypedArrayBuffer.wrap(response.asInstanceOf[ArrayBuffer]))
  }

  def apply(f: NanoboardMessageData ⇒ Unit): NanoboardMessageStream = {
    new NanoboardMessageStream(f)
  }
}

// WebSocket wrapper
final class NanoboardMessageStream(f: NanoboardMessageData ⇒ Unit) {
  private var webSocket: Option[WebSocket] = None
  private var last = Set.empty[String]
  private var lastSet = false

  def setContext(hashes: Set[String]): Unit = {
    if (!lastSet || hashes != last) {
      webSocket.foreach { webSocket ⇒
        val buffer = NanoboardMessageStream.asArrayBuffer(BinaryMarshaller.write(hashes))
        webSocket.send(buffer)
      }
    }
    last = hashes
    lastSet = webSocket.isDefined
  }

  private def initWebSocket(): Unit = {
    val webSocket = new WebSocket(s"ws://${window.location.host}/live")

    webSocket.binaryType = "arraybuffer"

    webSocket.onmessage = { (m: MessageEvent) ⇒
      val message = NanoboardMessageStream.asMessage(m.data)
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
