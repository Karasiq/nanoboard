package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.locales.BoardLocale
import org.scalajs.jquery.jQuery
import org.widok.moment.Moment
import rx._

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport
object NanoboardFrontend extends JSApp {
  implicit val ec: ExecutionContext = JSExecutionContext.queue
  implicit val ctx = implicitly[Ctx.Owner]

  @JSExport
  override def main(): Unit = {
    jQuery(() ⇒ {
      Moment.locale(BoardLocale.browserLanguage)
      NanoboardController().initialize()
    })
  }
}
