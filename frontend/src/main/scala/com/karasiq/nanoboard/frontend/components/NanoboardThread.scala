package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{ButtonBuilder, ButtonGroup, ButtonGroupSize}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.components.post.NanoboardPost
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardContext, NanoboardMessageData}
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

object NanoboardThread {
  def apply(postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): NanoboardThread = {
    new NanoboardThread(postsPerPage)
  }
}

trait NanoboardPostContainer {
  val context: Var[NanoboardContext]
  val posts: Var[Vector[NanoboardMessageData]]
  def update(): Unit
}

final class NanoboardThread(postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] with NanoboardPostContainer {
  override val context: Var[NanoboardContext] = NanoboardContext.fromLocation()

  override val posts = Var(Vector.empty[NanoboardMessageData])

  override def update(): Unit = {
    val future = context.now match {
      case NanoboardContext.Root ⇒
        NanoboardApi.categories()

      case NanoboardContext.Thread(hash, offset) ⇒
        NanoboardApi.thread(hash, offset, postsPerPage)
    }

    future.onComplete {
      case Success(posts) ⇒
        this.posts.update(posts)

      case Failure(exc) ⇒
        println(s"Nanoboard thread error: $exc")
        this.posts.update(Vector.empty)
    }
  }

  context.foreach(_ ⇒ update())

  private val threadPosts = Rx[Frag] {
    val thread = posts()
    val rendered = for {
      opPost ← thread.headOption.map(NanoboardPost(context() != NanoboardContext.Root, _))
      answers ← Some(thread.tail.map(NanoboardPost(false, _)))
    } yield opPost +: answers

    div(for (p ← rendered.toVector.flatten) yield GridSystem.mkRow(p))
  }

  private val pagination = Rx[Frag] {
    context() match {
      case NanoboardContext.Root ⇒
        ""

      case NanoboardContext.Thread(hash, offset) ⇒
        ButtonGroup(ButtonGroupSize.small,
          if (offset > 0) ButtonBuilder()("angle-double-left".fontAwesome(FontAwesome.fixedWidth), "Previous page", onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Thread(hash, math.max(0, offset - postsPerPage))
          }) else (),
          if (posts().length > postsPerPage) ButtonBuilder()("Next page", "angle-double-right".fontAwesome(FontAwesome.fixedWidth), onclick := Bootstrap.jsClick { _ ⇒
            context() = NanoboardContext.Thread(hash, math.max(0, offset + postsPerPage))
          }) else ()
        )
    }
  }

  override def renderTag(md: Modifier*): RenderedTag = {
    div(GridSystem.mkRow(threadPosts), GridSystem.mkRow(pagination), marginBottom := 400.px, md)
  }
}
