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

  def body: Cls
  def post: Cls
  def postInner: Cls
  def postId: Cls
  def postLink: Cls
  def input: Cls
  def submit: Cls

  def spoiler: Cls
  def greenText: Cls

  def hiddenScroll: StyleTree = {
    new Selector(Seq("::-webkit-scrollbar")).apply(display.none)
  }
}

object BoardStyle {
  val Makaba = Sheet[Makaba]
  val Neutron = Sheet[Neutron]
  val Muon = Sheet[Muon]

  def styles: Seq[BoardStyle] = Vector(Makaba, Neutron, Muon)

  def fromString(style: String): BoardStyle = styles.find(_.toString == style).getOrElse(Makaba)

  def selector(implicit ctx: Ctx.Owner): Selector = {
    new Selector()
  }

  final class Selector(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Style] {
    val style: Var[BoardStyle] = Var {
      val styleName: UndefOr[String] = window.localStorage.getItem("nanoboard-style")
      styleName.map(fromString).getOrElse(Makaba)
    }

    style.foreach { style ⇒
      window.localStorage.setItem("nanoboard-style", style.toString)
    }


    override def renderTag(md: Modifier*) = {
      tags2.style(style.map(_.styleSheetText), md)
    }
  }
}
