package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.Bootstrap
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.FormInput
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.navbar.{NavigationBar, NavigationBarStyle, NavigationTab}
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.api.streaming.NanoboardMessageStream
import com.karasiq.nanoboard.frontend.components._
import com.karasiq.nanoboard.frontend.locales.BoardLocale
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.utils.Scroll
import com.karasiq.nanoboard.streaming.NanoboardSubscription.{PostHashes, Unfiltered}
import com.karasiq.nanoboard.streaming.{NanoboardEvent, NanoboardSubscription}
import org.scalajs.dom._
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

object NanoboardController {
  def apply()(implicit ec: ExecutionContext, ctx: Ctx.Owner): NanoboardController = {
    new NanoboardController()
  }
}

final class NanoboardController(implicit ec: ExecutionContext, ctx: Ctx.Owner) {
  private implicit def controller: NanoboardController = this

  private val styleSelector = BoardStyle.selector

  val style: BoardStyle = styleSelector.style.now

  val locale = BoardLocale.fromBrowserLanguage()

  private val thread = ThreadContainer(NanoboardContext.fromLocation(), postsPerPage = 20)

  private val settingsPanel = SettingsPanel()

  private val pngGenerationPanel = PngGenerationPanel()

  private val title = ThreadPageTitle(thread.model)

  private val styleField = FormInput.select(locale.style, BoardStyle.styles.map(_.toString):_*)

  styleField.selected() = Seq(style.toString)

  styleField.selected.map(_.head).foreach { style ⇒
    styleSelector.style() = BoardStyle.fromString(style)
  }

  private val navigationBar = NavigationBar()
    .withBrand("Nanoboard", onclick := Bootstrap.jsClick { _ ⇒
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
    case NanoboardEvent.PostAdded(message, pending) ⇒
      // Notifications.info(s"New message: ${message.text}", Layout.topRight)
      thread.model.addPost(message)
      if (pending) {
        pngGenerationPanel.model.addPost(message)
      }

    case NanoboardEvent.PostDeleted(hash) ⇒
      // Notifications.warning(s"Post was deleted: $hash", Layout.topRight)
      deleteSingle(NanoboardMessageData(None, None, hash, "", 0))
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
    if (!Scroll.to(s"#post-$hash")) {
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
    Seq(thread.model, pngGenerationPanel.model).foreach(_.addPost(post))
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
