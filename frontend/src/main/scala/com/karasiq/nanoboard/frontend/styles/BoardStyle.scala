package com.karasiq.nanoboard.frontend.styles

import scala.scalajs.js.UndefOr

import org.scalajs.dom._
import rx._
import scalatags.JsDom.tags2
import scalatags.JsDom.all._
import scalatags.stylesheet.{StyleSheet, _}

import com.karasiq.bootstrap.Bootstrap.default._

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
  lazy val styles: Seq[BoardStyle] = {
    Vector(Makaba, Futaba, Burichan, Muon, Neutron, Gurochan)
  }

  def fromString(style: String): BoardStyle = {
    styles.find(_.toString == style).getOrElse(Makaba)
  }

  def selector: Selector = {
    new Selector()
  }

  final class Selector extends BootstrapHtmlComponent {
    val style: Var[BoardStyle] = Var {
      val styleName: UndefOr[String] = window.localStorage.getItem("nanoboard-style")
      styleName.map(fromString).getOrElse(Makaba)
    }

    style.foreach { style ⇒
      window.localStorage.setItem("nanoboard-style", style.toString)
    }

    override def renderTag(md: Modifier*) = {
      tags2.style(style.map(_.styleSheetText), CommonStyles.styleSheetText, md)
    }
  }
}
