package com.karasiq.nanoboard.frontend.utils

import scala.scalajs.js

object Scroll {
  def to(selector: String): Boolean = {
    import org.scalajs.jquery.jQuery
    val e = jQuery(selector)
    if (e.length > 0) {
      jQuery("html, body").animate(scalajs.js.Dynamic.literal(
        scrollTop = e.offset().asInstanceOf[js.Dynamic].top
      ), 800)
      true
    } else {
      false
    }
  }
}
