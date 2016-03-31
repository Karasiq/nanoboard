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
}

object BoardStyle {
  val Common = Sheet[CommonStyles]
  val Makaba = Sheet[Makaba]
  val Futaba = Sheet[Futaba]
  val Burichan = Sheet[Burichan]
  val Muon = Sheet[Muon]
  val Neutron = Sheet[Neutron]
  val Gurochan = Sheet[Gurochan]

  def styles: Seq[BoardStyle] = Vector(Makaba, Futaba, Burichan, Muon, Neutron, Gurochan)

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
      tags2.style(style.map(_.styleSheetText), Common.styleSheetText, md)
    }
  }
}
