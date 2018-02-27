package com.karasiq.nanoboard.frontend

import rx._

import com.karasiq.nanoboard.frontend.utils.RxLocation

sealed trait NanoboardContext
sealed trait NanoboardContextWithOffset extends NanoboardContext {
  def offset: Int
  def withOffset(newOffset: Int): NanoboardContextWithOffset
}
object NanoboardContext {
  case object Categories extends NanoboardContext
  case class Thread(hash: String, offset: Int = 0) extends NanoboardContextWithOffset {
    def withOffset(newOffset: Int) = copy(offset = newOffset)
  }
  case class Recent(offset: Int = 0) extends NanoboardContextWithOffset {
    def withOffset(newOffset: Int) = copy(offset = newOffset)
  }
  case class Pending(offset: Int = 0) extends NanoboardContextWithOffset {
    def withOffset(newOffset: Int) = copy(offset = newOffset)
  }

  //noinspection VariablePatternShadow
  // Simple single page app router
  def fromLocation()(implicit ctx: Ctx.Owner): Var[NanoboardContext] = {
    val location = RxLocation()
    val sha256 = "([a-fA-F0-9]{32})".r
    val sha256WithOffset = "([a-fA-F0-9]{32})/(\\d+)".r
    val onlyOffset = "(\\d+)".r
    val pendingOffset = "pending/(\\d+)".r
    val result = Var[NanoboardContext](NanoboardContext.Categories)
    location.hash.foreach { hash ⇒
      result() = hash match {
        case Some(sha256(hash)) ⇒
          NanoboardContext.Thread(hash)

        case Some(sha256WithOffset(hash, offset)) ⇒
          NanoboardContext.Thread(hash, offset.toInt)

        case Some(onlyOffset(offset)) ⇒
          NanoboardContext.Recent(offset.toInt)

        case Some("pending") ⇒
          NanoboardContext.Pending()

        case Some(pendingOffset(offset)) ⇒
          NanoboardContext.Pending(offset.toInt)

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

        case NanoboardContext.Pending(0) ⇒
          Some("pending")

        case NanoboardContext.Pending(offset) ⇒
          Some(s"pending/$offset")
      }
    }
    result
  }
}
