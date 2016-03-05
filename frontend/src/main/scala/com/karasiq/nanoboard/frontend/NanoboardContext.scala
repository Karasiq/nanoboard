package com.karasiq.nanoboard.frontend

import com.karasiq.nanoboard.frontend.utils.RxLocation
import rx._

sealed trait NanoboardContext
object NanoboardContext {
  case object Categories extends NanoboardContext
  case class Thread(hash: String, offset: Int) extends NanoboardContext
  case class Recent(offset: Int) extends NanoboardContext

  // Simple single page app router
  def fromLocation()(implicit ctx: Ctx.Owner): Var[NanoboardContext] = {
    val location = RxLocation()
    val sha256 = "([A-Za-z0-9]{32})".r
    val sha256WithOffset = "([A-Za-z0-9]{32})/(\\d+)".r
    val onlyOffset = "(\\d+)".r
    val result = Var[NanoboardContext](NanoboardContext.Categories)
    location.hash.foreach { hash ⇒
      result() = hash match {
        case Some(sha256(h)) ⇒
          NanoboardContext.Thread(h, 0)

        case Some(sha256WithOffset(h, offset)) ⇒
          NanoboardContext.Thread(h, offset.toInt)

        case Some(onlyOffset(offset)) ⇒
          NanoboardContext.Recent(offset.toInt)

        case _ ⇒
          NanoboardContext.Categories
      }
    }
    result.triggerLater {
      location.hash() = result.now match {
        case NanoboardContext.Categories ⇒
          None

        case NanoboardContext.Thread(hash, 0) ⇒
          Some(s"$hash")

        case NanoboardContext.Thread(hash, offset) ⇒
          Some(s"$hash/$offset")

        case NanoboardContext.Recent(offset) ⇒
          Some(offset.toString)
      }
    }
    result
  }
}
