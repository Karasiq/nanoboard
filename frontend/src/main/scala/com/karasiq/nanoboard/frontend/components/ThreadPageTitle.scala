package com.karasiq.nanoboard.frontend.components

import rx._
import scalatags.JsDom.tags2
import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import com.karasiq.nanoboard.frontend.components.post.PostRenderer
import com.karasiq.nanoboard.frontend.model.ThreadModel
import com.karasiq.nanoboard.frontend.utils.PostParser

object ThreadPageTitle {
  def apply(thread: ThreadModel)(implicit controller: NanoboardController): ThreadPageTitle = {
    new ThreadPageTitle(thread)
  }
}

private[components] final class ThreadPageTitle(thread: ThreadModel)(implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  import controller.locale

  val title = Rx[String] {
    thread.context() match {
      case NanoboardContext.Thread(_, _) ⇒
        thread.posts().headOption.fold(locale.nanoboard) { post ⇒
          val text = Some(PostParser.parse(post.text))
            .map(PostRenderer.strip(_).trim.split("\\s+").take(10).mkString(" "))
            .filter(_.nonEmpty)
          text.fold(locale.nanoboard)(locale.nanoboard + " - " + _)
        }

      case NanoboardContext.Recent(0) ⇒
        s"${locale.nanoboard} - ${locale.recentPosts}"

      case NanoboardContext.Recent(offset) ⇒
        s"${locale.nanoboard} - ${locale.recentPostsFrom(offset)}"

      case NanoboardContext.Categories ⇒
        s"${locale.nanoboard} - ${locale.categories}"

      case _ ⇒
        locale.nanoboard
    }

  }

  override def renderTag(md: Modifier*) = {
    tags2.title(title, md)
  }
}
