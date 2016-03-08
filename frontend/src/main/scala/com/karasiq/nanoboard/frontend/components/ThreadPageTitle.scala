package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapHtmlComponent
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.NanoboardContext
import com.karasiq.nanoboard.frontend.components.post.PostRenderer
import com.karasiq.nanoboard.frontend.utils.PostParser
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.tags2

object ThreadPageTitle {
  def apply(thread: PostsContainer)(implicit ctx: Ctx.Owner): ThreadPageTitle = {
    new ThreadPageTitle(thread)
  }
}

private[components] final class ThreadPageTitle(thread: PostsContainer)(implicit ctx: Ctx.Owner) extends BootstrapHtmlComponent[dom.html.Title] {
  val title = Rx[String] {
    thread.context() match {
      case NanoboardContext.Thread(_, _) ⇒
        thread.posts().headOption.fold("Nanoboard") { post ⇒
          val text = Some(PostParser.parse(post.text))
            .map(PostRenderer.asPlainText(_).trim.split("\\s+").take(10).mkString(" "))
            .filter(_.nonEmpty)
          text.fold("Nanoboard")("Nanoboard - " + _)
        }

      case NanoboardContext.Recent(0) ⇒
        "Nanoboard - Recent posts"

      case NanoboardContext.Recent(offset) ⇒
        s"Nanoboard - Recent posts (from $offset)"

      case NanoboardContext.Categories ⇒
        "Nanoboard - Categories"
    }

  }

  override def renderTag(md: Modifier*) = {
    tags2.title(title, md)
  }
}
