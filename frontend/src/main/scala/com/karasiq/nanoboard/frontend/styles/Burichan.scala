package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._

trait Burichan extends BoardStyle {
  override def toString: String = {
    "Burichan"
  }

  override def body = cls(
    color := "#000000",
    backgroundColor := "#EEF2FF"
  )

  override def post = cls(
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

  override def postInner = cls(
    maxHeight := 48.em,
    overflowX.hidden,
    overflowY.auto,
    marginBottom := 0.5.em,
    fontSize := 0.9.em,
    fontFamily := "Verdana,sans-serif",
    whiteSpace.`pre-wrap`,
    wordWrap.`break-word`,
    hiddenScroll
  )

  override def postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  override def postLink = cls(
    color := "#34345C",
    &.hover(
      color.red
    ),
    cursor.pointer,
    marginRight := 0.5.em
  )

  override def input = cls()

  override def submit = cls()

  override def greenText = cls(
    color.green,
    fontSize := 90.pct,
    lineHeight := 2.em
  )

  override def spoiler = cls(
    textDecoration.none,
    color := "#9988EE",
    background := "#9988EE",
    &.hover(
      color := "#34345C"
    )
  )
}
