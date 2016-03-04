package com.karasiq.nanoboard.frontend.styles

import com.karasiq.bootstrap.BootstrapHtmlComponent
import com.karasiq.bootstrap.BootstrapImplicits._
import org.scalajs.dom
import org.scalajs.dom._
import rx._

import scala.scalajs.js.UndefOr
import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import scalatags.stylesheet.{StyleSheet, _}

trait BoardStyle extends StyleSheet {
  override final def customSheetName: Option[String] = Some("nanoboard")
  def post: Cls
  def postInner: Cls
  def postId: Cls
  def postLink: Cls

  def spoiler: Cls
  def greenText: Cls
}

object BoardStyle {
  val Makaba = Sheet[Makaba]

  def selector(implicit ctx: Ctx.Owner): Selector = {
    new Selector()
  }

  final class Selector(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Style] {
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


    override def renderTag(md: Modifier*) = {
      tags2.style(style.map(_.styleSheetText), md)
    }
  }
}
