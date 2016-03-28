package com.karasiq.nanoboard.frontend.utils

import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import org.parboiled2._

sealed trait PostDomValue
object PostDomValue {
  case class PostDomValues(seq: Seq[PostDomValue]) extends PostDomValue
  case class PlainText(underlying: String) extends PostDomValue
  case class BBCode(name: String, parameters: Map[String, String], inner: PostDomValue) extends PostDomValue
  case class ShortBBCode(name: String, value: String) extends PostDomValue
  case class Markdown(value: String) extends PostDomValue
}

object PostParser {
  def parse(text: String): PostDomValue = {
    new PostParser(text).Message.run().getOrElse(PlainText(text))
  }
}

class PostParser(val input: ParserInput) extends Parser {
  def Literal: Rule1[PostDomValue.PlainText] = rule { "[plain]" ~ capture(oneOrMore(!"[/plain]" ~ ANY)) ~ "[/plain]" ~> PostDomValue.PlainText }
  def Markdown: Rule1[PostDomValue.Markdown] = rule { "[md]" ~ capture(oneOrMore(!"[/md]" ~ ANY)) ~ "[/md]" ~> PostDomValue.Markdown }
  def BBCodeParameter: Rule1[(String, String)] = rule { capture(oneOrMore(CharPredicate.Alpha)) ~ '=' ~ '"' ~ capture(zeroOrMore(!'"' ~ ANY)) ~ '"'  ~> ((s1: String, s2: String) ⇒ s1 → s2) }
  def BBCode: Rule1[PostDomValue.BBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ zeroOrMore(' ' ~ BBCodeParameter) ~ ']' ~> ((tag: String, parameters: Seq[(String, String)]) ⇒ push(tag) ~ push(parameters.toMap) ~ FormattedText ~ ("[/" ~ tag ~ ']')) ~> PostDomValue.BBCode }
  def ShortBBCode: Rule1[PostDomValue.ShortBBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ '=' ~ capture(oneOrMore(!']' ~ ANY)) ~ ']' ~> PostDomValue.ShortBBCode }
  def PlainText: Rule1[PostDomValue.PlainText] = rule { capture(oneOrMore(!('[' ~ optional('/') ~ oneOrMore(CharPredicate.Alpha) ~ optional('=' ~ oneOrMore(!']' ~ ANY)) ~ zeroOrMore(' ' ~ BBCodeParameter) ~ ']') ~ ANY)) ~> PostDomValue.PlainText }
  def FormattedText: Rule1[PostDomValues] = rule { zeroOrMore(Literal | Markdown | BBCode | ShortBBCode | PlainText) ~> PostDomValues }
  def Message = rule { FormattedText ~ EOI }
}
