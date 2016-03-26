package com.karasiq.nanoboard.frontend.utils

import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import org.parboiled2._

sealed trait PostDomValue
object PostDomValue {
  case class PostDomValues(seq: Seq[PostDomValue]) extends PostDomValue
  case class PlainText(underlying: String) extends PostDomValue
  case class BBCode(name: String, inner: PostDomValue) extends PostDomValue
  case class ShortBBCode(name: String, value: String) extends PostDomValue
  case class Markdown(value: String) extends PostDomValue
}

object PostParser {
  def parse(text: String): PostDomValue = {
    new PostParser(text).Message.run().getOrElse(PlainText(text))
  }
}

class PostParser(val input: ParserInput) extends Parser {
  def Markdown: Rule1[PostDomValue.Markdown] = rule { "[md]" ~ capture(oneOrMore(!"[/md]" ~ ANY)) ~ "[/md]" ~> PostDomValue.Markdown }
  def BBCode: Rule1[PostDomValue.BBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ ']' ~> ((tag: String) ⇒ push(tag) ~ FormattedText ~ ("[/" ~ tag ~ ']')) ~> PostDomValue.BBCode }
  def ShortBBCode: Rule1[PostDomValue.ShortBBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ '=' ~ capture(oneOrMore(!']' ~ ANY)) ~ ']' ~> PostDomValue.ShortBBCode }
  def PlainText: Rule1[PostDomValue.PlainText] = rule { capture(oneOrMore(!('[' ~ optional('/') ~ oneOrMore(CharPredicate.Alpha) ~ optional('=' ~ oneOrMore(!']' ~ ANY)) ~ ']') ~ ANY)) ~> PostDomValue.PlainText }
  def FormattedText: Rule1[PostDomValues] = rule { zeroOrMore(Markdown | BBCode | ShortBBCode | PlainText) ~> PostDomValues }
  def Message = rule { FormattedText ~ EOI }
}
