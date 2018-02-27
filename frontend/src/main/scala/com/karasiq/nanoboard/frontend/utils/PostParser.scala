package com.karasiq.nanoboard.frontend.utils

import org.parboiled2._

import com.karasiq.nanoboard.frontend.utils.PostDomValue._

sealed trait PostDomValue
object PostDomValue {
  case class PostDomValues(seq: Seq[PostDomValue]) extends PostDomValue
  case class PlainText(underlying: String) extends PostDomValue
  case class BBCode(name: String, parameters: Map[String, String], inner: PostDomValue) extends PostDomValue
  case class ShortBBCode(name: String, value: String) extends PostDomValue
}

object PostParser {
  def parse(text: String, plainCodes: Set[String] = Set("md", "plain", "code", "file", "img", "video")): PostDomValue = {
    new PostParser(text, plainCodes).Message.run().getOrElse(PlainText(text))
  }
}

class PostParser(val input: ParserInput, plainCodes: Set[String]) extends Parser {
  private def BBCodeParameter: Rule1[(String, String)] = rule { capture(oneOrMore(CharPredicate.Alpha)) ~ (('=' ~ '"' ~ capture(zeroOrMore(!'"' ~ ANY)) ~ '"') | push("")) ~> { (s1: String, s2: String) ⇒ s1 → s2 } }
  private def BBCodeParameters: Rule1[Map[String, String]] = rule { zeroOrMore(' ' ~ BBCodeParameter) ~> { (parameters: Seq[(String, String)]) ⇒ parameters.toMap } }
  private def BBCodeAnyTag: Rule0 = rule { '[' ~ optional('/') ~ oneOrMore(CharPredicate.Alpha) ~ (('=' ~ oneOrMore(!']' ~ ANY)) | BBCodeParameters) ~ ']' }
  private def BBCodeOpenTag: Rule2[String, Map[String, String]] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ BBCodeParameters ~ ']' }
  private def BBCodeCloseTag(tag: String): Rule0 = rule { "[/" ~ tag ~ "]" }

  def BBCode: Rule1[PostDomValue.BBCode] = rule {
    BBCodeOpenTag ~>
    { (tag: String, parameters: Map[String, String]) ⇒
      push(tag) ~ push(parameters) ~
      ((test(plainCodes.contains(tag.toLowerCase) || parameters.contains("plain")) ~ capture(oneOrMore(!BBCodeCloseTag(tag) ~ ANY)) ~> PostDomValue.PlainText) | FormattedText) ~ BBCodeCloseTag(tag)
    } ~> PostDomValue.BBCode
  }

  def ShortBBCode: Rule1[PostDomValue.ShortBBCode] = rule { '[' ~ capture(oneOrMore(CharPredicate.Alpha)) ~ '=' ~ capture(oneOrMore(!']' ~ ANY)) ~ ']' ~> PostDomValue.ShortBBCode }
  def PlainText: Rule1[PostDomValue.PlainText] = rule { capture(oneOrMore(!BBCodeAnyTag ~ ANY)) ~> PostDomValue.PlainText }
  def FormattedText: Rule1[PostDomValues] = rule { zeroOrMore(BBCode | ShortBBCode | PlainText) ~> PostDomValues }
  def Message = rule { FormattedText ~ EOI }
}
