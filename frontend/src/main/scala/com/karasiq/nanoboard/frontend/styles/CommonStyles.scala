package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._
import scalatags.stylesheet.{Selector, StyleSheet}

trait CommonStyles extends StyleSheet {
  def flatScroll = cls(
    overflowX.hidden,
    overflowY.auto,
    whiteSpace.`pre-wrap`,
    wordWrap.`break-word`,
    new Selector(Seq("::-webkit-scrollbar")).apply(display.none)
  )
}
