package com.karasiq.nanoboard.frontend

import scala.language.postfixOps

import moment.Moment
import org.scalajs.jquery.jQuery

import com.karasiq.nanoboard.frontend.locales.BoardLocale
import com.karasiq.taboverridejs.TabOverride

object NanoboardFrontend {
  def main(args: Array[String]): Unit = {
    jQuery(() ⇒ {
      TabOverride.tabSize(2)
      Moment.locale(BoardLocale.browserLanguage)
      NanoboardController().initialize()
    })
  }
}
