package com.karasiq.nanoboard.frontend.components.post.actions

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.nanoboard.frontend.api.NanoboardApi

private[post] object PendingButton {
  def apply(post: NanoboardMessageData)(implicit controller: NanoboardController) = {
    new PendingButton(post)
  }
}

private[post] final class PendingButton(post: NanoboardMessageData)(implicit controller: NanoboardController) extends BootstrapComponent {
  import controller.{locale, style}

  override def render(md: Modifier*): Modifier = {
    controller.isPending(post.hash).map { pending ⇒
      if (!pending) a(style.postLink, href := "#", Icons.enqueue, locale.enqueue, onclick := Callback.onClick { a ⇒
        NanoboardApi.markAsPending(post.hash).foreach { _ ⇒
          controller.addPending(post)
        }
      }) else a(style.postLink, href := "#", Icons.dequeue, locale.dequeue, onclick := Callback.onClick { a ⇒
        NanoboardApi.markAsNotPending(post.hash).foreach { _ ⇒
          controller.deletePending(post)
        }
      })
    }
  }
}
