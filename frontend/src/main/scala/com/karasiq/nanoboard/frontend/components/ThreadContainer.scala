package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{ButtonBuilder, ButtonGroup, ButtonGroupSize, ButtonStyle}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.components.post.{NanoboardPost, PostRenderer}
import com.karasiq.nanoboard.frontend.model.ThreadModel
import com.karasiq.nanoboard.frontend.utils.PostParser
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardContextWithOffset, NanoboardController}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

object ThreadContainer {
  def apply(context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): ThreadContainer = {
    new ThreadContainer(context, postsPerPage)
  }
}

final class ThreadContainer(val context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
  import controller.locale

  val model = ThreadModel(context, postsPerPage)

  // View
  private val threadPosts = Rx[Frag] {
    val thread = model.posts()
    val rendered = context.now match {
      case NanoboardContext.Thread(_, _) ⇒
        val (opPost, answers) = thread.splitAt(1)
        opPost.map(NanoboardPost(true, false, _)) ++ answers.map(NanoboardPost(false, true, _))

      case NanoboardContext.Recent(_) | NanoboardContext.Pending(_) ⇒
        thread.map(NanoboardPost(true, true, _))

      case NanoboardContext.Categories ⇒
        thread.map(NanoboardPost(false, true, _))
    }
    div(for (p ← rendered) yield GridSystem.mkRow(p))
  }

  private val pagination = Rx[Frag] {
    val posts = model.posts()
    val deleted = model.deletedPosts()

    def previousButton(ofs: NanoboardContextWithOffset, prevOffset: Int): Tag  = {
      ButtonBuilder(ButtonStyle.danger)(
        "angle-double-left".fontAwesome(FontAwesome.fixedWidth),
        locale.fromTo(prevOffset, prevOffset + postsPerPage),
        onclick := Bootstrap.jsClick { _ ⇒
          context() = ofs.withOffset(prevOffset)
        })
    }

    def nextButton(ofs: NanoboardContextWithOffset, newOffset: Int): Tag = {
      ButtonBuilder(ButtonStyle.success)(
        locale.fromTo(newOffset, newOffset + postsPerPage),
        "angle-double-right".fontAwesome(FontAwesome.fixedWidth),
        onclick := Bootstrap.jsClick { _ ⇒
          context() = ofs.withOffset(math.max(0, newOffset))
        })
    }

    context.now match {
      case NanoboardContext.Categories ⇒
        ""

      case th @ NanoboardContext.Thread(hash, offset) ⇒
        ButtonGroup(ButtonGroupSize.default,
          if (offset > 0) previousButton(th, math.max(0, offset - postsPerPage)) else (),
          if ((posts.length + deleted - 1) >= postsPerPage) nextButton(th, offset + math.max(0, posts.length - 1)) else (),
          margin := 5.px
        )

      case ofs: NanoboardContextWithOffset ⇒
        ButtonGroup(ButtonGroupSize.default,
          if (ofs.offset > 0) previousButton(ofs, math.max(0, ofs.offset - postsPerPage)) else (),
          if ((posts.length + deleted) >= postsPerPage) nextButton(ofs, ofs.offset + posts.length) else (),
          margin := 5.px
        )
    }
  }

  override def renderTag(md: Modifier*): RenderedTag = {
    val categories = Rx[Frag] {
      span(
        model.categories().map[Frag, Seq[Frag]] {
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
