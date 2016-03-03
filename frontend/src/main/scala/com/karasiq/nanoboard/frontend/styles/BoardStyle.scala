package com.karasiq.nanoboard.frontend.styles

import scalatags.stylesheet._

trait BoardStyle extends StyleSheet {
  override final def customSheetName: Option[String] = Some("nanoboard")
  def post: Cls
  def postInner: Cls
  def postId: Cls
  def postLink: Cls

  def spoiler: Cls
  def greenText: Cls
}
