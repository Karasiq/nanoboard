package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._
import scalatags.generic.Style

object Neutron extends BoardStyle {
  initStyleSheet()
  
  val body = cls(
    color := "#698CC0",
    backgroundColor := "#212121"
  )

  val post = cls(
    minWidth := 40.pct,
    maxWidth := 100.pct,
    border := "solid 1px #575757",
    borderRadius := 2.px,
    display.`inline-block`,
    background := "#212121",
    color := "#698CC0",
    margin := 0.25.em,
    clear.both,
    padding := "0.5em 1.5em"
  )

  val postInner = cls(
    fontFamily := "Trebuchet MS,Trebuchet,tahoma,serif",
    marginBottom := 0.5.em,
    fontSize := 1.em
  )

  val postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  val postLink = cls(
    color := "#C9BE89",
    &.hover(
      color := "#EEFEBB"
    ),
    cursor.pointer,
    marginRight := 0.5.em
  )

  val input = cls(
    backgroundColor := "#111111 !important",
    color := "#CCCCCC",
    border := "2px solid #545454!important"
  )

  val submit = cls(
    Style("webkitAppearance", "-webkit-appearance") := "none!important",
    backgroundColor := "#333333!important",
    backgroundImage := "-webkit-gradient(linear,center top,center bottom,from(#4A4A4A),color-stop(25%,#313131),color-stop(50%,#292929),color-stop(75%,#313131),to(#4A4A4A))!important",
    borderRadius := "5px!important",
    borderBottom := "1px solid #151515!important",
    borderTop := "1px solid #151515!important",
    borderLeft := "1px solid #000000!important",
    borderRight := "1px solid #000000!important",
    color := "#AAAAAA!important",
    fontWeight := "bold!important"
  )

  val greenText = cls(
    color.green,
    fontSize := 90.pct,
    lineHeight := 2.em
  )

  val spoiler = cls(
    textDecoration.none,
    color := "#575757",
    background := "#575757",
    &.hover(
      color := "#C9BE89"
    )
  )

  override def toString: String = {
    "Neutron"
  }
}
