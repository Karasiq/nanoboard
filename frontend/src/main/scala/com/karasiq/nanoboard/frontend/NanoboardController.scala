package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.navbar.{NavigationBar, NavigationTab}
import com.karasiq.nanoboard.frontend.api.{NanoboardCategory, NanoboardMessageData, NanoboardMessageStream}
import com.karasiq.nanoboard.frontend.components._
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import org.scalajs.dom._
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

object NanoboardController {
  def apply()(implicit ec: ExecutionContext, ctx: Ctx.Owner): NanoboardController = {
    new NanoboardController()
  }
}

final class NanoboardController(implicit ec: ExecutionContext, ctx: Ctx.Owner) extends PostsContainer {
  private implicit def controller: NanoboardController = this

  private val styleSelector = BoardStyle.selector

  val style: BoardStyle = styleSelector.style.now

  private val thread = ThreadContainer(NanoboardContext.fromLocation(), postsPerPage = 20)

  private val settingsPanel = SettingsPanel()

  private val pngGenerationPanel = PngGenerationPanel()

  private val title = ThreadPageTitle(thread)

  private val navigationBar = NavigationBar(
    NavigationTab("Nanoboard", "thread", "server".fontAwesome(FontAwesome.fixedWidth), div("container-fluid".addClass, thread)),
    NavigationTab("Server settings", "server-settings", "wrench".fontAwesome(FontAwesome.fixedWidth), div("container".addClass, settingsPanel)),
    NavigationTab("Container generation", "png-gen", "camera-retro".fontAwesome(FontAwesome.fixedWidth), div("container".addClass, pngGenerationPanel))
  )

  private val messageChannel = NanoboardMessageStream { message ⇒
    // Notifications.info(s"New message: ${message.text}", Layout.topRight)
    thread.addPost(message)
  }

  def initialize(): Unit = {
    document.head.appendChild(controller.title.renderTag().render)
    Seq[Frag](navigationBar.navbar("Nanoboard"), div(marginTop := 70.px, navigationBar.content), controller.styleSelector.renderTag(id := "nanoboard-style"))
      .foreach(_.applyTo(document.body))
  }

  def updateCategories(newList: Seq[NanoboardCategory]): Unit = {
    thread.updateCategories()
  }

  def setContext(context: NanoboardContext): Unit = {
    thread.context() = context
    navigationBar.selectTab("thread")
  }

  def isPending(hash: String): Rx[Boolean] = {
    pngGenerationPanel.posts.map(_.exists(_.hash == hash))
  }

  def addPending(post: NanoboardMessageData): Unit = {
    pngGenerationPanel.addPost(post)
  }

  def deletePending(post: NanoboardMessageData): Unit = {
    pngGenerationPanel.deletePost(post)
  }

  override def context: Rx[NanoboardContext] = thread.context

  override def posts: Rx[Vector[NanoboardMessageData]] = thread.posts

  override def update(): Unit = {
    Seq(thread, pngGenerationPanel).foreach(_.update())
  }

  override def addPost(post: NanoboardMessageData): Unit = {
    Seq(thread, pngGenerationPanel).foreach(_.addPost(post))
  }

  override def deletePost(post: NanoboardMessageData): Unit = {
    Seq(thread, pngGenerationPanel).foreach(_.deletePost(post))
  }

  private val messageChannelContext = Rx {
    posts().map(_.hash).toSet ++ thread.categories().map(_.hash).toSet ++ Some(thread.context()).collect {
      case NanoboardContext.Thread(hash, _) ⇒
        hash
    }
  }

  messageChannelContext.foreach { context ⇒
    messageChannel.setContext(context)
  }
}
