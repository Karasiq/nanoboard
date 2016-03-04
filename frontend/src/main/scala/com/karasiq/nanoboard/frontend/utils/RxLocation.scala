package com.karasiq.nanoboard.frontend.utils

import org.scalajs.dom
import org.scalajs.dom.window
import org.scalajs.jquery.jQuery
import rx._

import scala.scalajs.js.UndefOr

final class RxLocation(implicit ctx: Ctx.Owner) {
  private def readHash(hash: UndefOr[String]): Option[String] = {
    hash
      .filter(_.nonEmpty)
      .map(_.tail)
      .toOption
  }

  val hash: Var[Option[String]] = Var(readHash(window.location.hash))

  hash.triggerLater {
    window.location.hash = hash.now.fold("")("#" + _)
  }

  jQuery(dom.window).on("hashchange", () ⇒ {
    hash() = readHash(window.location.hash)
  })
}

object RxLocation {
  def apply()(implicit ctx: Ctx.Owner): RxLocation = {
    new RxLocation()
  }
}
