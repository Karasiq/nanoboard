package com.karasiq.nanoboard.frontend.utils

import org.scalajs.dom
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scalatags.JsDom.all._

object Mouse {
  def scroll(selector: String): Boolean = {
    import org.scalajs.jquery.jQuery
    val e = jQuery(selector)
    if (e.length > 0) {
      jQuery("html, body").animate(js.Dynamic.literal(
        scrollTop = e.offset().asInstanceOf[js.Dynamic].top
      ), 800)
      true
    } else {
      false
    }
  }

  def relative(xOffset: Double = 0, yOffset: Double = 0): Modifier = {
    Seq[Modifier](
      position.fixed,
      overflow.hidden,
      new Modifier {
        override def applyTo(t: Element): Unit = {
          dom.window.addEventListener("mousemove", (e: MouseEvent) ⇒ {
            val style = t.asInstanceOf[dom.html.Element].style
            style.left = (e.clientX + xOffset) + "px"
            style.top = (e.clientY + yOffset) + "px"
          })
        }
      }
    )
  }
}
