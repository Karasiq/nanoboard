package com.karasiq.nanoboard.frontend.utils

import com.karasiq.nanoboard.frontend.utils.PostDomValue._
import org.parboiled2._

sealed trait PostDomValue
object PostDomValue {
  case class PostDomValues(seq: Seq[PostDomValue]) extends PostDomValue
  case class PlainText(underlying: String) extends PostDomValue
  case class BBCode(name: String, inner: PostDomValue) extends PostDomValue
  case class ShortBBCode(name: String, value: String) extends PostDomValue
}

class PostParser(val input: ParserInput) extends Parser {
  private def bbcode: Rule1[BBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ ']' ~> ((tag: String) ⇒ push(tag) ~ FormattedText ~ ("[/" ~ tag ~ ']')) ~> BBCode }
  private def shortBbcode: Rule1[ShortBBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ '=' ~ capture(oneOrMore(!']' ~ ANY)) ~ ']' ~> ShortBBCode }
  private def plainText: Rule1[PlainText] = rule { capture(oneOrMore(!('[' ~ optional('/') ~ oneOrMore(CharPredicate.Alpha) ~ optional('=' ~ oneOrMore(!']' ~ ANY)) ~ ']') ~ ANY)) ~> PlainText }
  def FormattedText: Rule1[PostDomValues] = rule { zeroOrMore(bbcode | shortBbcode | plainText) ~> PostDomValues }
  def Message = rule { FormattedText ~ EOI }
}
