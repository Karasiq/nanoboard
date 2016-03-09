package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{ButtonBuilder, ButtonGroup, ButtonGroupSize, ButtonStyle}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.api.{NanoboardApi, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.components.post.{NanoboardPost, PostRenderer}
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.utils.{Notifications, PostParser}
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import org.scalajs.dom
import rx._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

object ThreadContainer {
  def apply(context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): ThreadContainer = {
    new ThreadContainer(context, postsPerPage)
  }
}

final class ThreadContainer(val context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] with PostsContainer {
  import controller.locale

  // Model
  private val deletedPosts = Var(0)
  val categories = Var(Vector.empty[NanoboardMessageData])
  override val posts = Var(Vector.empty[NanoboardMessageData])

  // Controller
  override def addPost(post: NanoboardMessageData): Unit = {
    categories() = categories.now.collect {
      case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
        msg.copy(answers = answers + 1)

      case msg ⇒
        msg
    }

    val newPosts = context.now match {
      case NanoboardContext.Recent(0) ⇒
        if (posts.now.length == postsPerPage) {
          post +: posts.now.dropRight(1)
        } else {
          post +: posts.now
        }

      case NanoboardContext.Thread(hash, 0) if post.parent.contains(hash) ⇒
        val (opPost, answers) = posts.now.partition(_.hash == hash)
        if (answers.length >= postsPerPage) {
          opPost ++ Some(post) ++ answers.dropRight(1)
        } else {
          opPost ++ Some(post) ++ answers
        }

      case NanoboardContext.Thread(post.hash, _) ⇒
        val (_, answers) = posts.now.partition(_.hash == post.hash)
        post +: answers

      case _ ⇒
        posts.now
    }

    posts() = newPosts.collect {
      case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
        msg.copy(answers = answers + 1)

      case msg ⇒
        msg
    }
  }

  override def deletePost(post: NanoboardMessageData): Unit = {
    categories() = categories.now.filterNot(_.hash == post.hash)
    context.now match {
      case NanoboardContext.Thread(post.hash, _) ⇒
        context() = post.parent.fold[NanoboardContext](NanoboardContext.Categories)(NanoboardContext.Thread(_))

      case NanoboardContext.Thread(_, _) ⇒
        val (Vector(opPost), answers) = posts.now.splitAt(1)
        posts() = opPost.copy(answers = opPost.answers - 1) +: answers.filterNot(p ⇒ p.hash == post.hash || p.parent.contains(post.hash))
        deletedPosts() = deletedPosts.now + (answers.length - posts.now.length + 1)

      case _ ⇒
        val current = posts.now
        posts() = current.filterNot(p ⇒ p.hash == post.hash || p.parent.contains(post.hash))
        deletedPosts() = deletedPosts.now + (current.length - posts.now.length)
    }
  }

  override def update(): Unit = {
    val future = context.now match {
      case NanoboardContext.Categories ⇒
        Future.successful(categories.now)

      case NanoboardContext.Thread(hash, offset) ⇒
        NanoboardApi.thread(hash, offset, postsPerPage)

      case NanoboardContext.Recent(offset) ⇒
        NanoboardApi.recent(offset, postsPerPage)
    }

    future.onComplete {
      case Success(posts) ⇒
        this.deletedPosts() = 0
        this.posts() = posts

      case Failure(exc) ⇒
        Notifications.error(exc)(locale.updateError, Layout.topRight)
    }
  }

  def updateCategories(): Unit = {
    NanoboardApi.categories().onComplete {
      case Success(categories) ⇒
        this.categories() = categories

      case Failure(exc) ⇒
        Notifications.error(exc)(locale.updateError, Layout.topRight)
    }
  }

  // Initialization
  context.foreach(_ ⇒ update())
  categories.foreach { categories ⇒
    if (context.now == NanoboardContext.Categories) {
      deletedPosts() = 0
      posts() = categories
    }
  }
  updateCategories()

  // View
  private val threadPosts = Rx[Frag] {
    val thread = posts()
    val rendered = context.now match {
      case NanoboardContext.Thread(_, _) ⇒
        val (opPost, answers) = thread.splitAt(1)
        opPost.map(NanoboardPost(true, false, _)) ++ answers.map(NanoboardPost(false, true, _))

      case NanoboardContext.Recent(_) ⇒
        thread.map(NanoboardPost(true, true, _))

      case NanoboardContext.Categories ⇒
        thread.map(NanoboardPost(false, true, _))
    }
    div(for (p ← rendered) yield GridSystem.mkRow(p))
  }

  private val pagination = Rx[Frag] {
    val posts = this.posts()
    val deleted = this.deletedPosts()
    context.now match {
      case NanoboardContext.Categories ⇒
        ""

      case NanoboardContext.Thread(hash, offset) ⇒
        val offsetAdd = math.max(0, posts.length - 1)
        val prevOffset = math.max(0, offset - postsPerPage)
        val previousButton = ButtonBuilder(ButtonStyle.info)(
          "angle-double-left".fontAwesome(FontAwesome.fixedWidth),
          locale.fromTo(prevOffset, prevOffset + postsPerPage),
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Thread(hash, prevOffset)
          })
        val nextButton = ButtonBuilder(ButtonStyle.info)(
          locale.fromTo(offset + offsetAdd, offset + offsetAdd + postsPerPage),
          "angle-double-right".fontAwesome(FontAwesome.fixedWidth),
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Thread(hash, math.max(0, offset + offsetAdd))
          })

        ButtonGroup(ButtonGroupSize.small,
          if (offset > 0) previousButton else (),
          if ((posts.length + deleted - 1) >= postsPerPage) nextButton else (),
          margin := 5.px
        )

      case NanoboardContext.Recent(offset) ⇒
        val offsetAdd = posts.length
        val prevOffset = math.max(0, offset - postsPerPage)
        val previousButton = ButtonBuilder(ButtonStyle.info)(
          "angle-double-left".fontAwesome(FontAwesome.fixedWidth),
          locale.fromTo(prevOffset, prevOffset + postsPerPage),
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Recent(prevOffset)
          })
        val nextButton = ButtonBuilder(ButtonStyle.info)(
          locale.fromTo(offset + offsetAdd, offset + offsetAdd + postsPerPage),
          "angle-double-right".fontAwesome(FontAwesome.fixedWidth),
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Recent(math.max(0, offset + offsetAdd))
          })

        ButtonGroup(ButtonGroupSize.small,
          if (offset > 0) previousButton else (),
          if ((posts.length + deleted) >= postsPerPage) nextButton else (),
          margin := 5.px
        )
    }
  }

  override def renderTag(md: Modifier*): RenderedTag = {
    val categories = Rx[Frag] {
      span(
        this.categories().map[Frag, Seq[Frag]] {
          case NanoboardMessageData(_, hash, text, answers) ⇒
            val plainText = PostRenderer.asPlainText(PostParser.parse(text))
            val answersSpan = span(marginLeft := 0.25.em, "envelope-o".fontAwesome(FontAwesome.fixedWidth), answers)
            a(href := s"#$hash", margin := 0.25.em, "[", span(fontWeight.bold, plainText), answersSpan, "]", onclick := Bootstrap.jsClick { _ ⇒
              controller.setContext(NanoboardContext.Thread(hash))
            })
        }
      )
    }

    val navigation = Seq(
      a(href := "#0", margin := 0.25.em, "newspaper-o".fontAwesome(FontAwesome.fixedWidth), locale.recentPosts, onclick := Bootstrap.jsClick { _ ⇒
        controller.setContext(NanoboardContext.Recent())
      }),
      a(href := "#", margin := 0.25.em, "sitemap".fontAwesome(FontAwesome.fixedWidth), locale.categories, onclick := Bootstrap.jsClick { _ ⇒
        controller.setContext(NanoboardContext.Categories)
      })
    )
    div(GridSystem.mkRow(categories), GridSystem.mkRow(navigation), GridSystem.mkRow(pagination), GridSystem.mkRow(threadPosts), GridSystem.mkRow(pagination), marginBottom := 400.px, md)
  }
}
