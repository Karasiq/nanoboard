package com.karasiq.nanoboard.frontend.utils

import java.io.{PrintWriter, StringWriter}

import org.scalajs.dom.console

// From proxychecker frontend
object Notifications {
  import scala.scalajs.js.Dynamic.{global => js, literal => lt}
  import scala.scalajs.js.{Array => JsArray}

  private def defaultTimeout: Int = {
    1800
  }

  sealed trait Layout {
    def apply(): String
  }

  object Layout {
    private final class LayoutImpl(string: String) extends Layout {
      override def apply(): String = string
    }

    def top: Layout = new LayoutImpl("top")
    def topLeft: Layout = new LayoutImpl("topLeft")
    def topRight: Layout = new LayoutImpl("topRight")
    def topCenter: Layout = new LayoutImpl("topCenter")

    def center: Layout = new LayoutImpl("center")
    def centerLeft: Layout = new LayoutImpl("centerLeft")
    def centerRight: Layout = new LayoutImpl("centerRight")

    def bottom: Layout = new LayoutImpl("bottom")
    def bottomLeft: Layout = new LayoutImpl("bottomLeft")
    def bottomRight: Layout = new LayoutImpl("bottomRight")
    def bottomCenter: Layout = new LayoutImpl("bottomCenter")
  }

  sealed class InfoMessage(`type`: String) {
    def apply(text: String, layout: Layout = Layout.top, timeout: Int = defaultTimeout): Unit = {
      Notifications.notify(text, `type`=`type`, layout=layout, timeout = Some(timeout))
    }
  }

  sealed class ErrorMessage(cause: Option[Throwable]) extends InfoMessage("error") {
    private def formatException(title: String, exc: Option[Throwable]): String = {
      val stringWriter = new StringWriter(256)
      val printWriter = new PrintWriter(stringWriter)
      try {
        printWriter.println(title)
        exc.foreach(_.printStackTrace(printWriter))
        printWriter.flush()
        stringWriter.toString
      } finally printWriter.close()
    }

    override def apply(text: String, layout: Layout = Layout.top, timeout: Int = defaultTimeout): Unit = {
      val formatted = formatException(text, cause)
      console.error(formatted)
      super.apply(formatted, layout, timeout)
    }
  }

  def alert: InfoMessage = new InfoMessage("alert")
  def success: InfoMessage = new InfoMessage("success")
  def error(cause: Throwable): InfoMessage = new ErrorMessage(Some(cause))
  def error: InfoMessage = new ErrorMessage(None)
  def warning: InfoMessage = new InfoMessage("warning")
  def info: InfoMessage = new InfoMessage("information")

  sealed class ConfirmationMessage {
    def apply(text: String, layout: Layout = Layout.top)(onSuccess: ⇒ Unit): Unit = {
      Notifications.notify(text, `type`="confirmation", layout=layout, buttons=JsArray(
        button("Ok", "btn btn-primary") { msg ⇒
          msg.close()
          onSuccess
        },

        button("Cancel", "btn btn-danger") { msg ⇒
          msg.close()
        }
      ))
    }
  }

  def confirmation = new ConfirmationMessage

  private def button(text: String, addClass: String = "")(onClick: scalajs.js.Dynamic ⇒ Unit): scalajs.js.Dynamic = {
    lt(addClass=addClass, text=text, onClick=onClick)
  }

  private def notify(text: String, `type`: String = "alert", theme: String = "defaultTheme",
                     layout: Layout = Layout.top, timeout: Option[Int] = None,
                     buttons: JsArray[scalajs.js.Dynamic] = JsArray()) = {
    js.noty(lt(
      `type`=`type`,
      text = text,
      layout = layout(),
      theme = theme,
      timeout = if (timeout.nonEmpty) timeout.get else false,
      animation = lt(
        open = lt(height="toggle"),
        close = lt(height="toggle"),
        easing = "swing", // easing
        speed = 500 // opening & closing animation speed
      ),
      buttons = if (buttons.nonEmpty) buttons else false
    ))
  }
}
