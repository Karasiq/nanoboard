package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._

trait Gurochan extends BoardStyle {
  override def toString: String = {
    "Gurochan"
  }

  override def body = cls(
    color := "#000000",
    backgroundColor := "#EDDAD2"
  )

  override def post = cls(
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

  override def postInner = cls(
    marginBottom := 0.5.em,
    fontSize := 0.9.em,
    fontFamily := "Trebuchet MS, Verdana, sans-serif"
  )

  override def postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  override def postLink = cls(
    color := "#34345C",
    &.hover(
      color := "#DD0000"
    ),
    cursor.pointer,
    marginRight := 0.5.em
  )

  override def input = cls()

  override def submit = cls()

  override def greenText = cls(
    color := "#AF0A0F",
    fontSize := 90.pct,
    lineHeight := 2.em
  )

  override def spoiler = cls(
    textDecoration.none,
    color := "#CA927B",
    background := "#CA927B",
    &.hover(
      color := "#34345C"
    )
  )
}
