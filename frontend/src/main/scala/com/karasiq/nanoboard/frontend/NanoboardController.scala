package com.karasiq.nanoboard.frontend

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import org.scalajs.dom._
import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.api.streaming.NanoboardMessageStream
import com.karasiq.nanoboard.frontend.components._
import com.karasiq.nanoboard.frontend.locales.BoardLocale
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.utils.Mouse
import com.karasiq.nanoboard.streaming.{NanoboardEvent, NanoboardSubscription}
import com.karasiq.nanoboard.streaming.NanoboardSubscription.{PostHashes, Unfiltered}

object NanoboardController {
  def apply(): NanoboardController = {
    new NanoboardController()
  }
}

final class NanoboardController {
  private implicit def controller: NanoboardController = this

  private val styleSelector = BoardStyle.selector

  val style: BoardStyle = styleSelector.style.now

  val locale = BoardLocale.fromBrowserLanguage()

  private val thread = ThreadContainer(NanoboardContext.fromLocation(), postsPerPage = 20)

  private val settingsPanel = SettingsPanel()

  private val pngGenerationPanel = PngGenerationPanel()

  private val title = ThreadPageTitle(thread.model)

  private val styleField = FormInput.simpleSelect(locale.style, BoardStyle.styles.map(_.toString):_*)

  styleField.selected() = Seq(style.toString)

  styleField.selected.map(_.head).foreach { style ⇒
    styleSelector.style() = BoardStyle.fromString(style)
  }

  private val navigationBar = NavigationBar()
    .withBrand("Nanoboard", onclick := Callback.onClick { _ ⇒
      setContext(NanoboardContext.Categories)
    })
    .withTabs(
      NavigationTab(locale.nanoboard, "thread", Icons.thread, GridSystem.containerFluid(thread)),
      NavigationTab(locale.settings, "server-settings", Icons.settings, GridSystem.container(
        GridSystem.mkRow(styleField.renderTag(style.input)),
        GridSystem.mkRow(settingsPanel)
      )),
      NavigationTab(locale.containerGeneration, "png-gen", Icons.container, GridSystem.container(GridSystem.mkRow(pngGenerationPanel)))
    )
    .withStyles(NavigationBarStyle.staticTop, NavigationBarStyle.inverse)
    .withContentContainer(md ⇒ div(md))
    .build()

  private val messageChannel = NanoboardMessageStream {
    case NanoboardEvent.PostAdded(message) ⇒
      // Notifications.info(s"New message: ${message.text}", Layout.topRight)
      addPost(message)

    case NanoboardEvent.PostDeleted(hash) ⇒
      // Notifications.warning(s"Post was deleted: $hash", Layout.topRight)
      deleteSingle(NanoboardMessageData(None, None, hash, "", 0))

    case NanoboardEvent.PostVerified(message) ⇒
      addPending(message)
  }

  def initialize(): Unit = {
    document.head.appendChild(controller.title.renderTag().render)
    Seq[Modifier](navigationBar, style.body, controller.styleSelector.renderTag(id := "nanoboard-style"))
      .foreach(_.applyTo(document.body))
  }

  def updateCategories(): Unit = {
    thread.model.updateCategories()
  }

  def updatePosts(): Unit = {
    Seq(thread.model, pngGenerationPanel.model).foreach(_.updatePosts())
  }

  def showPost(hash: String): Unit = {
    if (!Mouse.scroll(s"#post-$hash")) {
      setContext(NanoboardContext.Thread(hash))
    }
  }

  def setContext(context: NanoboardContext): Unit = {
    thread.context() = context
    navigationBar.selectTab("thread")
  }

  def isPending(hash: String): Rx[Boolean] = {
    pngGenerationPanel.model.posts.map(_.exists(_.hash == hash))
  }

  def addPending(post: NanoboardMessageData): Unit = {
    pngGenerationPanel.model.addPost(post)
  }

  def deletePending(post: NanoboardMessageData): Unit = {
    pngGenerationPanel.model.deleteSingle(post)
  }

  def addPost(post: NanoboardMessageData): Unit = {
    thread.model.addPost(post)
  }

  def deleteSingle(post: NanoboardMessageData): Unit = {
    Seq(thread.model, pngGenerationPanel.model).foreach(_.deleteSingle(post))
  }

  private val messageChannelContext = Rx[NanoboardSubscription] {
    thread.model.context() match {
      case NanoboardContext.Recent(0) | NanoboardContext.Pending(0) ⇒
        // Accept all posts
        Unfiltered

      case context ⇒
        PostHashes(thread.model.posts().map(_.hash).toSet ++ thread.model.categories().map(_.hash).toSet ++ Some(context).collect {
          case NanoboardContext.Thread(hash, _) ⇒
            hash
        })
    }
  }

  messageChannelContext.foreach { context ⇒
    messageChannel.setContext(context)
  }
}
