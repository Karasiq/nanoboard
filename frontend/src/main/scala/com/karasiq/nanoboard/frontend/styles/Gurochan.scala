package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._

object Gurochan extends BoardStyle {
  initStyleSheet()
  
  val body = cls(
    color := "#000000",
    backgroundColor := "#EDDAD2"
  )

  val post = cls(
    minWidth := 40.pct,
    maxWidth := 100.pct,
    border := "1px solid #CA927B",
    borderRadius := 2.px,
    display.`inline-block`,
    background := "#D9AF9E",
    margin := 0.25.em,
    clear.both,
    padding := "0.5em 1.5em"
  )

  val postInner = cls(
    marginBottom := 0.5.em,
    fontSize := 0.9.em,
    fontFamily := "Trebuchet MS, Verdana, sans-serif"
  )

  val postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  val postLink = cls(
    color := "#34345C",
    &.hover(
      color := "#DD0000"
    ),
    cursor.pointer,
    marginRight := 0.5.em
  )

  val input = cls()

  val submit = cls()

  val greenText = cls(
    color := "#AF0A0F",
    fontSize := 90.pct,
    lineHeight := 2.em
  )

  val spoiler = cls(
    textDecoration.none,
    color := "#CA927B",
    background := "#CA927B",
    &.hover(
      color := "#34345C"
    )
  )

  override def toString: String = {
    "Gurochan"
  }
}
