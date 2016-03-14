package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.{ButtonBuilder, ButtonGroup, ButtonGroupSize, ButtonStyle}
import com.karasiq.bootstrap.grid.GridSystem
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.api.NanoboardContainer
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import org.scalajs.dom
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

private[components] object ContainersPanel {
  def apply(perPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController): ContainersPanel = {
    new ContainersPanel(perPage)
  }
}

private[components] final class ContainersPanel(perPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Div] {
  import controller.locale
  val currentOffset = Var(0)
  val containers = Var(Vector.empty[NanoboardContainer])

  def update(): Unit = {
    NanoboardApi.containers(currentOffset.now, perPage).foreach { cs ⇒
      containers() = cs
    }
  }

  currentOffset.foreach(_ ⇒ update())

  def renderTag(md: Modifier*) = {
    def isEmpty(c: NanoboardContainer): Modifier = if (c.posts == 0) Seq(textDecoration.`line-through`, color.gray) else ()
    var loading = false
    div(containers.map { cs ⇒
      div(
        GridSystem.mkRow(
          ButtonGroup(ButtonGroupSize.extraSmall,
            ButtonBuilder(ButtonStyle.danger)(
              "angle-double-left".fontAwesome(FontAwesome.fixedWidth),
              locale.fromTo(math.max(0, currentOffset.now - perPage), currentOffset.now),
              onclick := Bootstrap.jsClick { _ ⇒
                currentOffset() = math.max(0, currentOffset.now - perPage)
              }
            ),
            ButtonBuilder(ButtonStyle.success)(
              locale.fromTo(currentOffset.now + containers.now.length, currentOffset.now + containers.now.length + perPage),
              "angle-double-right".fontAwesome(FontAwesome.fixedWidth),
              onclick := Bootstrap.jsClick { _ ⇒
                currentOffset() = currentOffset.now + containers.now.length
              }
            )
          )
        ),
        for (c ← cs) yield GridSystem.mkRow(isEmpty(c), a(href := "#", "ban".fontAwesome(FontAwesome.fixedWidth), onclick := Bootstrap.jsClick { _ ⇒
          if (!loading) {
            Notifications.confirmation(locale.batchDeleteConfirmation(c.posts), Layout.topLeft) {
              loading = true
              NanoboardApi.clearContainer(c.id).onComplete {
                case Success(_) ⇒
                  loading = false
                  this.update()
                  controller.updatePosts()
                  controller.updateCategories()
                  Notifications.success(locale.batchDeleteSuccess(c.posts), Layout.topRight)

                case Failure(exc) ⇒
                  loading = false
                  Notifications.error(exc)(locale.batchDeleteError, Layout.topRight)
              }
            }
          }
        }), s"${locale.container(c)} (", a(isEmpty(c), href := c.url, c.url, target := "_blank"), ")")
      )
    })
  }
}
