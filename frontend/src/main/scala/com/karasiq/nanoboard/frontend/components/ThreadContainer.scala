package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{ButtonBuilder, ButtonGroup, ButtonGroupSize, ButtonStyle}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardContext, NanoboardMessageData}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

object ThreadContainer {
  def apply(context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): ThreadContainer = {
    new ThreadContainer(context, postsPerPage)
  }
}

final class ThreadContainer(val context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] with PostsContainer {
  private val deletedPosts = Var(0)

  override val posts = Var(Vector.empty[NanoboardMessageData])

  override def addPost(post: NanoboardMessageData): Unit = {
    context.now match {
      case NanoboardContext.Recent(0) ⇒
        posts() = post +: posts.now

      case NanoboardContext.Thread(hash, 0) if post.parent.contains(hash) ⇒
        posts() = posts.now.take(1) ++ Seq(post) ++ posts.now.drop(1)

      case _ ⇒
        posts() = posts.now.collect {
          case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
            msg.copy(answers = answers + 1)

          case msg ⇒
            msg
        }
    }
  }

  override def deletePost(post: NanoboardMessageData): Unit = {
    context.now match {
      case NanoboardContext.Thread(post.hash, _) ⇒
        context() = post.parent.fold[NanoboardContext](NanoboardContext.Categories)(NanoboardContext.Thread(_, 0))

      case _ ⇒
        posts() = posts.now.filterNot(p ⇒ p.hash == post.hash || p.parent.contains(post.hash))
        deletedPosts() = deletedPosts.now + 1
    }
  }

  override def update(): Unit = {
    val future = context.now match {
      case NanoboardContext.Categories ⇒
        NanoboardApi.categories()

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
        Notifications.error(exc)("Nanoboard thread update error", Layout.topRight)
    }
  }

  context.foreach(_ ⇒ update())

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
        val offsetAdd = posts.length - 1
        val prevOffset = math.max(0, offset - postsPerPage)
        val previousButton = ButtonBuilder(ButtonStyle.info)(
          "angle-double-left".fontAwesome(FontAwesome.fixedWidth),
          s"From $prevOffset to ${prevOffset + postsPerPage}",
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Thread(hash, prevOffset)
          })
        val nextButton = ButtonBuilder(ButtonStyle.info)(
          s"From ${offset + offsetAdd} to ${offset + offsetAdd + postsPerPage}",
          "angle-double-right".fontAwesome(FontAwesome.fixedWidth),
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Thread(hash, math.max(0, offset + offsetAdd))
          })

        ButtonGroup(ButtonGroupSize.small,
          if (offset > 0) previousButton else (),
          if ((offset + offsetAdd) < posts.headOption.fold(0)(_.answers)) nextButton else (),
          margin := 5.px
        )

      case NanoboardContext.Recent(offset) ⇒
        val offsetAdd = posts.length
        val prevOffset = math.max(0, offset - postsPerPage)
        val previousButton = ButtonBuilder(ButtonStyle.info)(
          "angle-double-left".fontAwesome(FontAwesome.fixedWidth),
          s"From $prevOffset to ${prevOffset + postsPerPage}",
          onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Recent(prevOffset)
          })
        val nextButton = ButtonBuilder(ButtonStyle.info)(
          s"From ${offset + offsetAdd} to ${offset + offsetAdd + postsPerPage}",
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
    val navigation = Seq(
      a(href := "#0", margin := 0.25.em, "newspaper-o".fontAwesome(FontAwesome.fixedWidth), "Recent posts", onclick := Bootstrap.jsClick { _ ⇒
        controller.setContext(NanoboardContext.Recent(0))
      }),
      a(href := "#", margin := 0.25.em, "sitemap".fontAwesome(FontAwesome.fixedWidth), "Categories", onclick := Bootstrap.jsClick { _ ⇒
        controller.setContext(NanoboardContext.Categories)
      })
    )
    div(GridSystem.mkRow(navigation), GridSystem.mkRow(pagination), GridSystem.mkRow(threadPosts), GridSystem.mkRow(pagination), marginBottom := 400.px, md)
  }
}
