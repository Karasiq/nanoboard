package com.karasiq.nanoboard.frontend.components.post.actions

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.{Bootstrap, BootstrapComponent}
import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import rx._

import scala.concurrent.ExecutionContext
import scalatags.JsDom.all._

private[post] object PendingButton {
  def apply(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) = {
    new PendingButton(post)
  }
}

private[post] final class PendingButton(post: NanoboardMessageData)(implicit ctx: Ctx.Owner, ec: ExecutionContext, controller: NanoboardController) extends BootstrapComponent {
  import controller.{locale, style}

  override def render(md: Modifier*): Modifier = {
    controller.isPending(post.hash).map { pending ⇒
      if (!pending) a(style.postLink, href := "#", Icons.enqueue, locale.enqueue, onclick := Bootstrap.jsClick { a ⇒
        NanoboardApi.markAsPending(post.hash).foreach { _ ⇒
          controller.addPending(post)
        }
      }) else a(style.postLink, href := "#", Icons.dequeue, locale.dequeue, onclick := Bootstrap.jsClick { a ⇒
        NanoboardApi.markAsNotPending(post.hash).foreach { _ ⇒
          controller.deletePending(post)
        }
      })
    }
  }
}
