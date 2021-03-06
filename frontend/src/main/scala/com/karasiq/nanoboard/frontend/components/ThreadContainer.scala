package com.karasiq.nanoboard.frontend.components

import scala.language.postfixOps

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.{Icons, NanoboardContext, NanoboardContextWithOffset, NanoboardController}
import com.karasiq.nanoboard.frontend.components.post.{NanoboardPost, PostRenderer}
import com.karasiq.nanoboard.frontend.model.ThreadModel
import com.karasiq.nanoboard.frontend.utils.PostParser

object ThreadContainer {
  def apply(context: Var[NanoboardContext], postsPerPage: Int)
           (implicit controller: NanoboardController): ThreadContainer = {
    new ThreadContainer(context, postsPerPage)
  }
}

final class ThreadContainer(val context: Var[NanoboardContext], postsPerPage: Int)
                           (implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  import controller.locale

  val model = ThreadModel(context, postsPerPage)

  // View
  private val threadPosts = Rx[Frag] {
    val thread = model.posts()
    val rendered = context.now match {
      case NanoboardContext.Thread(hash, _) ⇒
        val (opPost, answers) = thread.partition(_.hash == hash)
        opPost.map(NanoboardPost(true, false, _, scrollable = true)) ++ answers.map(NanoboardPost(false, true, _, scrollable = true))

      case NanoboardContext.Recent(_) | NanoboardContext.Pending(_) ⇒
        thread.map(NanoboardPost(true, true, _, scrollable = true))

      case NanoboardContext.Categories ⇒
        thread.map(NanoboardPost(false, true, _, scrollable = true))
    }
    div(for (p ← rendered) yield GridSystem.mkRow(p))
  }

  private val pagination = Rx[Frag] {
    val posts = model.posts()
    val deleted = model.deletedPosts().size

    def previousButton(ofs: NanoboardContextWithOffset, prevOffset: Int): Tag  = {
      Button(ButtonStyle.danger)(
        Icons.previous,
        locale.fromTo(prevOffset, prevOffset + postsPerPage),
        onclick := Callback.onClick { _ ⇒
          context() = ofs.withOffset(prevOffset)
        })
    }

    def nextButton(ofs: NanoboardContextWithOffset, newOffset: Int): Tag = {
      Button(ButtonStyle.success)(
        locale.fromTo(newOffset, newOffset + postsPerPage),
        Icons.next,
        onclick := Callback.onClick { _ ⇒
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

  override def renderTag(md: Modifier*): TagT = {
    val categories = Rx[Frag] {
      span(
        model.categories().map[Frag, Seq[Frag]] {
          case m @ NanoboardMessageData(_, _, hash, _, answers, _, _) ⇒
            val plainText = PostRenderer.strip(PostParser.parse(m.text))
            val answersSpan = span(marginLeft := 0.25.em, Icons.answers, answers)
            a(href := s"#$hash", margin := 0.25.em, "[", span(fontWeight.bold, plainText), answersSpan, "]", onclick := Callback.onClick { _ ⇒
              controller.setContext(NanoboardContext.Thread(hash))
            })
        }
      )
    }

    val navigation = Seq(
      a(href := "#0", margin := 0.25.em, Icons.recent, locale.recentPosts, onclick := Callback.onClick { _ ⇒
        controller.setContext(NanoboardContext.Recent())
      }),
      a(href := "#", margin := 0.25.em, Icons.categories, locale.categories, onclick := Callback.onClick { _ ⇒
        controller.setContext(NanoboardContext.Categories)
      })
    )
    div(GridSystem.mkRow(categories), GridSystem.mkRow(navigation), GridSystem.mkRow(pagination), GridSystem.mkRow(threadPosts), GridSystem.mkRow(pagination), marginBottom := 400.px, md)
  }
}
