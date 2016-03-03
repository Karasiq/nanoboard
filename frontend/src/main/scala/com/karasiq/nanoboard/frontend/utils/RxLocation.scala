package com.karasiq.nanoboard.frontend.utils

import org.scalajs.dom
import org.scalajs.dom.window
import org.scalajs.jquery.jQuery
import rx._

import scala.scalajs.js.UndefOr

final class RxLocation(implicit ctx: Ctx.Owner) {
  private val hash_ : Var[UndefOr[String]] = Var(window.location.hash)

  val hash: Rx[Option[String]] = hash_.map { hash ⇒
    hash
      .filter(_.nonEmpty)
      .map(_.tail)
      .toOption
  }

  jQuery(dom.window).on("hashchange", () ⇒ {
    hash_() = window.location.hash
  })
}
