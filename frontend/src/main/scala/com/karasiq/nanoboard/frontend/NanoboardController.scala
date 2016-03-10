package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.Bootstrap
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.navbar.{NavigationBar, NavigationBarStyle, NavigationTab}
import com.karasiq.nanoboard.frontend.api.streaming.NanoboardSubscription.{PostHashes, Unfiltered}
import com.karasiq.nanoboard.frontend.api.streaming.{NanoboardEvent, NanoboardMessageStream, NanoboardSubscription}
import com.karasiq.nanoboard.frontend.api.{NanoboardCategory, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.components._
import com.karasiq.nanoboard.frontend.locales.BoardLocale
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

  val locale = BoardLocale.fromBrowserLanguage()

  private val thread = ThreadContainer(NanoboardContext.fromLocation(), postsPerPage = 20)

  private val settingsPanel = SettingsPanel()

  private val pngGenerationPanel = PngGenerationPanel()

  private val title = ThreadPageTitle(thread)

  private val navigationBar = NavigationBar()
    .withBrand("Nanoboard", onclick := Bootstrap.jsClick { _ ⇒
      setContext(NanoboardContext.Categories)
    })
    .withTabs(
      NavigationTab(locale.nanoboard, "thread", "server".fontAwesome(FontAwesome.fixedWidth), div("container-fluid".addClass, thread)),
      NavigationTab(locale.settings, "server-settings", "wrench".fontAwesome(FontAwesome.fixedWidth), div("container".addClass, settingsPanel)),
      NavigationTab(locale.containerGeneration, "png-gen", "camera-retro".fontAwesome(FontAwesome.fixedWidth), div("container".addClass, pngGenerationPanel))
    )
    .withStyles(NavigationBarStyle.staticTop, NavigationBarStyle.inverse)
    .withContentContainer(div(marginTop := 70.px))
    .build()

  private val messageChannel = NanoboardMessageStream {
    case NanoboardEvent.PostAdded(message, pending) ⇒
      // Notifications.info(s"New message: ${message.text}", Layout.topRight)
      thread.addPost(message)
      if (pending) {
        pngGenerationPanel.addPost(message)
      }

    case NanoboardEvent.PostDeleted(hash) ⇒
      // Notifications.warning(s"Post was deleted: $hash", Layout.topRight)
      deletePost(NanoboardMessageData(None, hash, "", 0))
  }

  def initialize(): Unit = {
    document.head.appendChild(controller.title.renderTag().render)
    Seq[Modifier](navigationBar, controller.styleSelector.renderTag(id := "nanoboard-style"))
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
    pngGenerationPanel.posts() = pngGenerationPanel.posts.now.filterNot(_.hash == post.hash)
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

  private val messageChannelContext = Rx[NanoboardSubscription] {
    thread.context() match {
      case NanoboardContext.Recent(0) ⇒
        // Accept all posts
        Unfiltered

      case context ⇒
        PostHashes(posts().map(_.hash).toSet ++ thread.categories().map(_.hash).toSet ++ Some(context).collect {
          case NanoboardContext.Thread(hash, _) ⇒
            hash
        })
    }
  }

  messageChannelContext.foreach { context ⇒
    messageChannel.setContext(context)
  }
}
