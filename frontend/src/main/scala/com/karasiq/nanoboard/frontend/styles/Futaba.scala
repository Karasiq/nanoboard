package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._

trait Futaba extends BoardStyle {
  override def toString: String = {
    "Futaba"
  }

  override def body = cls(
    color := "#800000",
    backgroundColor := "#FFFFEE"
  )

  override def post = cls(
    minWidth := 40.pct,
    maxWidth := 100.pct,
    border := "solid 1px #F0D0B6",
    borderRadius := 2.px,
    display.`inline-block`,
    background := "#F0E0D6",
    color := "#800000",
    margin := 0.25.em,
    clear.both,
    padding := "0.5em 1.5em"
  )

  override def postInner = cls(
    marginBottom := 0.5.em,
    fontSize := 0.9.em,
    fontFamily := "Verdana,sans-serif"
  )

  override def postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  override def postLink = cls(
    color := "#0000EE",
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
    color := "#F0D0B6",
    background := "#F0D0B6",
    &.hover(
      color := "#0000EE"
    )
  )
}
