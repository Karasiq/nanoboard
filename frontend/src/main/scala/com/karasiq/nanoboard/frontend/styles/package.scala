package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import org.scalajs.dom.{Element, document, window}
import rx._

import scala.scalajs.js.UndefOr
import scalatags.JsDom.all._
import scalatags.stylesheet._

package object styles {
  val Makaba = Sheet[Makaba]

  final class StyleSelector(implicit ctx: Ctx.Owner) extends Modifier {
    val style: Var[BoardStyle] = Var {
      val styleName: UndefOr[String] = window.localStorage.getItem("nanoboard-style")
      styleName.collect {
        case "Makaba" ⇒
          Makaba
      }.getOrElse(Makaba)
    }

    style.foreach { style ⇒
      window.localStorage.setItem("nanoboard-style", style match {
        case _ ⇒
          "Makaba"
      })
    }

    def applyTo(body: Element): Unit = {
      import scalatags.JsDom.tags2
      document.body.appendChild(tags2.style(this.style.map(_.styleSheetText)).render)
    }
  }
}
