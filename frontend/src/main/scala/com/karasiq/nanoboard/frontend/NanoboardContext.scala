package com.karasiq.nanoboard.frontend

import com.karasiq.nanoboard.frontend.utils.RxLocation
import rx._

sealed trait NanoboardContext
object NanoboardContext {
  case object Root extends NanoboardContext
  case class Thread(hash: String, offset: Int) extends NanoboardContext

  def fromLocation()(implicit ctx: Ctx.Owner): Var[NanoboardContext] = {
    val location = RxLocation()
    val sha256 = "([A-Za-z0-9]{32})".r
    val sha256WithOffset = "([A-Za-z0-9]{32})/(\\d+)".r
    val result = Var[NanoboardContext](NanoboardContext.Root)
    location.hash.foreach { hash ⇒
      result() = hash match {
        case Some(sha256(h)) ⇒
          NanoboardContext.Thread(h, 0)

        case Some(sha256WithOffset(h, offset)) ⇒
          NanoboardContext.Thread(h, offset.toInt)

        case _ ⇒
          NanoboardContext.Root
      }
    }
    result.triggerLater {
      location.hash() = result.now match {
        case NanoboardContext.Root ⇒
          None

        case NanoboardContext.Thread(hash, 0) ⇒
          Some(s"$hash")

        case NanoboardContext.Thread(hash, offset) ⇒
          Some(s"$hash/$offset")
      }
    }
    result
  }
}
