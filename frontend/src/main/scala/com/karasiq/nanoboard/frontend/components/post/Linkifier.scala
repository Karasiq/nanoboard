package com.karasiq.nanoboard.frontend.components.post

import scala.language.postfixOps
import scala.util.matching.Regex

import org.scalajs.dom.Element

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.videojs.VideoSource

sealed trait LinkifierNode extends Frag
case class InlineDom(frag: Frag) extends LinkifierNode {
  override def render = frag.render
  override def applyTo(t: Element) = frag.applyTo(t)
}
case class InlineText(str: String) extends LinkifierNode {
  private val frag: Frag = str
  override def render = frag.render
  override def applyTo(t: Element) = frag.applyTo(t)
}

object Linkifier {
  private val videoRegex = """\b(https?|ftp)://([-a-zA-Z0-9.]+)(/[-a-zA-Z0-9+&@#/%=~_|!:,.;]*\.(webm|mp4|ogv|3gp|avi|mov))(\?[a-zA-Z0-9+&@#/%=~_|!:,.;]*)?""".r
  private val youtubeRegex = """https?://(?:www\.)?youtu(?:be\.com/watch\?v=|\.be/)([\w\-]+)(&(amp;)?[\w\?=]*)?""".r
  private val urlRegex = """\b(?:(?:https?|ftp|file)://|www\.|ftp\.)[-а-яА-Яa-zA-Z0-9+&@#/%=~_|$?!:,.]*[а-яА-ЯA-Za-z0-9+&@#/%=~_|$]""".r
  private val postLinkRegex = """(?:>>|/expand/)([A-Za-z0-9]{32})""".r
  private val quoteRegex = """(^|\n)>[^\r\n]+""".r

  private def processText(text: String, regex: Regex, f: String ⇒ Frag): Seq[LinkifierNode] = {
    regex.findFirstMatchIn(text) match {
      case Some(rm) ⇒
        val matched = text.slice(rm.start, rm.end)
        val texts = Seq(text.take(rm.start), text.drop(rm.end))
          .flatMap(processText(_, regex, f))
        texts.take(1) ++ Seq(InlineDom(f(matched))) ++ texts.drop(1)

      case None ⇒
        Seq(InlineText(text))
    }
  }

  def inlineYoutube(text: String): Seq[LinkifierNode] = {
    processText(text, youtubeRegex, url ⇒ PostExternalVideo.youtube(url))
  }

  def inlineVideos(text: String): Seq[LinkifierNode] = {
    processText(text, videoRegex, {
      case url @ videoRegex(protocol, domain, file, ext, query) ⇒
        PostExternalVideo(url, VideoSource(s"video/$ext", url))
    })
  }

  def linkify(text: String): Seq[LinkifierNode] = {
    processText(text, urlRegex, url ⇒ a(href := url, url))
  }

  def postLinks(text: String)(implicit controller: NanoboardController): Seq[LinkifierNode] = {
    processText(text, postLinkRegex, {
      case postLinkRegex(hash) ⇒
        PostLink(hash).renderTag(Icons.link, hash)
    })
  }

  def quotes(text: String)(implicit controller: NanoboardController): Seq[LinkifierNode] = {
    processText(text, quoteRegex, quote ⇒ span(controller.style.greenText, quote))
  }

  def apply(text: String)(implicit controller: NanoboardController): Seq[LinkifierNode] = {
    Seq(inlineYoutube _, inlineVideos _, linkify _, postLinks _, quotes _).foldLeft(Seq[LinkifierNode](InlineText(text))) {
      case (nodes, f) ⇒
        nodes.flatMap {
          case dom @ InlineDom(_) ⇒
            Some(dom)

          case InlineText(data) ⇒
            f(data)
        }
    }
  }
}
