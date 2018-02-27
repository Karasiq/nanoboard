package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._

object Burichan extends BoardStyle {
  initStyleSheet()

  val body = cls(
    color := "#000000",
    backgroundColor := "#EEF2FF"
  )

  val post = cls(
    minWidth := 40.pct,
    maxWidth := 100.pct,
    border := "solid 1px #CCCCCC",
    borderRadius := 2.px,
    display.`inline-block`,
    background := "#D6DAF0",
    margin := 0.25.em,
    clear.both,
    padding := "0.5em 1.5em"
  )

  val postInner = cls(
    marginBottom := 0.5.em,
    fontSize := 0.9.em,
    fontFamily := "Verdana,sans-serif"
  )

  val postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  val postLink = cls(
    color := "#34345C",
    &.hover(
      color.red
    ),
    cursor.pointer,
    marginRight := 0.5.em
  )

  val input = cls()

  val submit = cls()

  val greenText = cls(
    color.green,
    fontSize := 90.pct,
    lineHeight := 2.em
  )

  val spoiler = cls(
    textDecoration.none,
    color := "#9988EE",
    background := "#9988EE",
    &.hover(
      color := "#34345C"
    )
  )

  override def toString: String = {
    "Burichan"
  }
}
