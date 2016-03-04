package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.navbar.{NavigationBar, NavigationTab}
import com.karasiq.nanoboard.frontend.components._
import org.scalajs.dom.document
import org.scalajs.jquery.jQuery
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object NanoboardFrontend extends JSApp {
  implicit val ec: ExecutionContext = JSExecutionContext.queue
  implicit val ctx = implicitly[Ctx.Owner]

  @JSExport
  override def main(): Unit = {
    jQuery(() ⇒ {
      val controller = new NanoboardController()
      val navigationBar = NavigationBar(
        NavigationTab("Nanoboard", "posts", "server".fontAwesome(FontAwesome.fixedWidth), div("container-fluid".addClass, controller.thread)),
        NavigationTab("Server settings", "server-settings", "wrench".fontAwesome(FontAwesome.fixedWidth), div("container".addClass, controller.settingsPanel)),
        NavigationTab("Container generation", "png-gen", "camera-retro".fontAwesome(FontAwesome.fixedWidth), div("container".addClass, controller.pngGenerationPanel))
      )
      document.head.appendChild(controller.title.renderTag().render)
      Seq[Frag](navigationBar.navbar("Nanoboard"), div(marginTop := 70.px, navigationBar.content), controller.styleSelector.renderTag(id := "nanoboard-style"))
        .foreach(_.applyTo(document.body))
    })
  }
}
