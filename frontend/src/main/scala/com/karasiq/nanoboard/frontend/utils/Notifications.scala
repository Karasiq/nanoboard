package com.karasiq.nanoboard.frontend.utils

// From proxychecker frontend
object Notifications {
  import scala.scalajs.js.Dynamic.{global => js, literal => lt}
  import scala.scalajs.js.{Array => JsArray}

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
    def apply(text: String, layout: Layout = Layout.top, timeout: Int = 800): Unit = {
      Notifications.notify(text, `type`=`type`, layout=layout, timeout = Some(timeout))
    }
  }

  def alert = new InfoMessage("alert")
  def success = new InfoMessage("success")
  def error = new InfoMessage("error")
  def warning = new InfoMessage("warning")
  def info = new InfoMessage("information")

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
