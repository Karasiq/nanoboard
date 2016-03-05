package com.karasiq.nanoboard.frontend.utils

import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import org.parboiled2._

sealed trait PostDomValue
object PostDomValue {
  case class PostDomValues(seq: Seq[PostDomValue]) extends PostDomValue
  case class PlainText(underlying: String) extends PostDomValue
  case class GreenText(underlying: PostDomValue) extends PostDomValue
  case class BoldText(underlying: PostDomValue) extends PostDomValue
  case class ItalicText(underlying: PostDomValue) extends PostDomValue
  case class UnderlinedText(underlying: PostDomValue) extends PostDomValue
  case class StrikeThroughText(underlying: PostDomValue) extends PostDomValue
  case class SpoilerText(underlying: PostDomValue) extends PostDomValue
  case class InlineImage(base64: String) extends PostDomValue
  case class ExternalImage(url: String) extends PostDomValue
  case class ExternalVideo(url: String) extends PostDomValue
  case class FractalMusic(data: String) extends PostDomValue
}

class PostParser(val input: ParserInput) extends Parser {
  def bbcode(tag: String): Rule1[PostDomValue] = rule { ignoreCase(s"[$tag]") ~ FormattedText ~ ignoreCase(s"[/$tag]") }
  def shortBbcode(tag: String): Rule1[String] = rule { ignoreCase(s"[$tag=") ~ capture(oneOrMore(!']' ~ ANY)) ~ ']' }

  def Green = rule { bbcode("g") ~> GreenText }
  def Bold = rule { bbcode("b") ~> BoldText }
  def Italic = rule { bbcode("i") ~> ItalicText }
  def Underlined = rule { bbcode("u") ~> UnderlinedText }
  def StrikeThrough = rule { bbcode("s") ~> StrikeThroughText }
  def Spoiler = rule { (bbcode("sp") | bbcode("spoiler")) ~> SpoilerText }
  def Image = rule { shortBbcode("img") ~> InlineImage }
  def ImageLink = rule { shortBbcode("simg") ~> ExternalImage }
  def VideoLink = rule { shortBbcode("svid") ~> ExternalVideo }
  def Music = rule { shortBbcode("fm") ~> FractalMusic }

  def Text = rule { capture(oneOrMore(!('[' ~ optional('/') ~ oneOrMore(CharPredicate.Alpha) ~ optional('=' ~ oneOrMore(!']' ~ ANY)) ~ ']') ~ ANY)) ~> PlainText }
  def FormattedText = rule { zeroOrMore(VideoLink | ImageLink | Music | Image | Green | Bold | Italic | Underlined | StrikeThrough | Spoiler | Text) ~> PostDomValues }

  def Message = rule { FormattedText ~ EOI }
}
