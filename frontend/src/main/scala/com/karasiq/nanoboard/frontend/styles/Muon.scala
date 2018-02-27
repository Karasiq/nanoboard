package com.karasiq.nanoboard.frontend.styles

import scalatags.Text.all._
import scalatags.generic.Style

object Muon extends BoardStyle {
  initStyleSheet()

  val body = cls(
    color := "#9B8165",
    background := "scroll #211F1A url('/img/muon_bg.jpg') repeat"
  )

  val post = cls(
    minWidth := 40.pct,
    maxWidth := 100.pct,
    border := "solid 1px #34352D",
    borderRadius := 2.px,
    display.`inline-block`,
    background := "url('/img/muon_posts.jpg') #292825",
    color := "#9B8165",
    margin := 0.25.em,
    clear.both,
    padding := "0.5em 1.5em"
  )

  val postInner = cls(
    fontFamily := "Verdana,sans-serif",
    marginBottom := 0.5.em,
    fontSize := 1.em
  )

  val postId = cls(
    color := "#789922",
    marginRight := 0.5.em
  )

  val postLink = cls(
    color := "#FFD97A",
    &.hover(
      color := "#FCE236"
    ),
    cursor.pointer,
    marginRight := 0.5.em
  )

  val input = cls(
    background := "#3E3C38 url('/img/muon_inputs.jpg') !important",
    color := "#FEC77D!important",
    border := "1px solid #44453D"
  )

  val submit = cls(
    Style("webkitAppearance", "-webkit-appearance") := "none!important",
    backgroundImage := "-webkit-gradient(linear,center top,center bottom,from(#4A4A4A),color-stop(25%,#313131),color-stop(50%,#292929),color-stop(75%,#313131),to(#4A4A4A))!important",
    backgroundColor := "#333333!important",
    borderRadius := "5px!important",
    borderBottom := "1px solid #151515!important",
    borderTop := "1px solid #151515!important",
    borderLeft := "1px solid #000000!important",
    borderRight := "1px solid #000000!important",
    color := "#AAAAAA!important"
  )

  val greenText = cls(
    color.green,
    fontSize := 90.pct,
    lineHeight := 2.em
  )

  val spoiler = cls(
    textDecoration.none,
    color := "#454545",
    background := "#454545",
    opacity := 0.5,
    &.hover(
      color := "#FFD97A"
    )
  )

  override def toString: String = {
    "Muon"
  }
}
