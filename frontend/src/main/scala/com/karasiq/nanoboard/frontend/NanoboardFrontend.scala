package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.navbar.{NavigationBar, NavigationTab}
import com.karasiq.nanoboard.frontend.components.{NanoboardPageTitle, NanoboardThread}
import com.karasiq.nanoboard.frontend.styles._
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
      val styleSelector = BoardStyle.selector
      val thread = NanoboardThread(200, styleSelector.style.now)
      val title = NanoboardPageTitle(thread)
      val navigationBar = NavigationBar(
        NavigationTab("Nanoboard", "posts", "server".fontAwesome(FontAwesome.fixedWidth), div("container-fluid".addClass, thread))
      )
      document.head.appendChild(title().render)
      Seq[Frag](navigationBar.navbar("Nanoboard"), div(marginTop := 50.px, navigationBar.content), styleSelector.renderTag(id := "nanoboard-style"))
        .foreach(_.applyTo(document.body))
    })
  }
}
