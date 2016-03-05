package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.components._
import org.scalajs.jquery.jQuery
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

// TODO: Localization
@JSExport
object NanoboardFrontend extends JSApp {
  implicit val ec: ExecutionContext = JSExecutionContext.queue
  implicit val ctx = implicitly[Ctx.Owner]

  @JSExport
  override def main(): Unit = {
    jQuery(() ⇒ {
      // Initializes the application
      NanoboardController().initialize()
    })
  }
}
