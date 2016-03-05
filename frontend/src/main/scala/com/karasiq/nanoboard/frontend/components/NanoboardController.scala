package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.{NanoboardCategory, NanoboardContext, NanoboardMessageData}
import rx._

import scala.concurrent.ExecutionContext

final class NanoboardController(implicit ec: ExecutionContext, ctx: Ctx.Owner) {
  private implicit def controller: NanoboardController = this

  val styleSelector = BoardStyle.selector

  val style: BoardStyle = styleSelector.style.now

  val thread = NanoboardThread(50)

  val settingsPanel = SettingsPanel()

  val pngGenerationPanel = PngGenerationPanel()

  val title = NanoboardPageTitle(thread)

  def updateCategories(newList: Seq[NanoboardCategory]): Unit = {
    if (thread.context.now == NanoboardContext.Root) {
      thread.update()
    }
  }

  def setContext(context: NanoboardContext): Unit = {
    thread.context() = context
  }

  def addPost(post: NanoboardMessageData): Unit = {
    val posts = thread.posts.now
    if (post.parent.contains(posts.head.hash)) {
      thread.posts() = posts.take(1) ++ Seq(post) ++ posts.drop(1)
    } else {
      thread.posts() = posts.collect {
        case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
          msg.copy(answers = answers + 1)

        case msg ⇒
          msg
      }
    }
    pngGenerationPanel.posts() = pngGenerationPanel.posts.now :+ post
  }

  def deletePost(post: NanoboardMessageData): Unit = {
    pngGenerationPanel.posts() = pngGenerationPanel.posts.now.filterNot(_.hash == post.hash)
    thread.context.now match {
      case NanoboardContext.Thread(post.hash, _) ⇒
        setContext(post.parent.fold[NanoboardContext](NanoboardContext.Root)(NanoboardContext.Thread(_, 0)))

      case _ ⇒
        thread.posts() = thread.posts.now.filterNot(_.hash == post.hash)
    }
  }
}
