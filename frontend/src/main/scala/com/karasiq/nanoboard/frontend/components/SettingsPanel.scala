package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.buttons.ButtonBuilder
import com.karasiq.bootstrap.form.{Form, FormInput}
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardCategory, NanoboardContext}
import org.scalajs.dom
import rx._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalatags.JsDom.all._

//noinspection VariablePatternShadow
final class SettingsPanel(thread: NanoboardThread)(implicit ctx: Ctx.Owner, ec: ExecutionContext) extends BootstrapHtmlComponent[dom.html.Div] {
  private val placesText = Var("")
  private val categoriesText = Var("")

  val places = Rx {
    val urls = placesText().lines.toVector
    if (urls.forall(_.matches("""\b(https?|ftp)://([-a-zA-Z0-9.]+)(/[-a-zA-Z0-9+&@#/%=~_|!:,.;]*)?(\?[a-zA-Z0-9+&@#/%=~_|!:,.;]*)?"""))) {
      urls
    } else {
      Vector.empty
    }
  }

  val categories = Rx {
    Try {
      val lines = categoriesText().lines.toVector
      assert(lines.length % 2 == 0)
      val categories = lines.grouped(2).map(seq ⇒ NanoboardCategory(seq.head, seq.last)).toVector
      assert(categories.forall(c ⇒ c.hash.matches("[A-Za-z0-9]{32}") && c.name.nonEmpty))
      categories
    }.getOrElse(Vector.empty)
  }

  private val loading = Var(false)

  private val buttonDisabled = Rx {
    loading() || categories().isEmpty || places().isEmpty
  }

  override def renderTag(md: Modifier*) = {
    div(
      Form(
        FormInput.textArea("Places", rows := 15, placeholder := "http://imageboard.com/b/12356.html", placesText.reactiveInput)("has-error".classIf(places.map(_.isEmpty))),
        FormInput.textArea("Categories", rows := 15, placeholder := "Category hash\nCategory name", categoriesText.reactiveInput)("has-error".classIf(categories.map(_.isEmpty)))
      ),
      ButtonBuilder(block = true)("Apply settings", "disabled".classIf(buttonDisabled), onclick := Bootstrap.jsClick { _ ⇒
        if (!buttonDisabled.now) {
          loading() = true
          Future.sequence(Seq(NanoboardApi.setCategories(categories.now), NanoboardApi.setPlaces(places.now))).onComplete {
            case Success(_) ⇒
              loading() = false
              if (thread.context.now == NanoboardContext.Root) {
                thread.update()
              }

            case Failure(exc) ⇒
              println(s"Settings update error: $exc")
              loading() = false
          }
        }
      })
    )
  }

  def update(): Unit = {
    NanoboardApi.places().foreach { places ⇒
      placesText() = places.mkString("\n")
    }

    NanoboardApi.categories().foreach { categories ⇒
      categoriesText() = categories.map(c ⇒ s"${c.hash}\n${c.text}").mkString("\n")
    }
  }

  update()

  thread.posts.foreach { posts ⇒
    if (thread.context.now == NanoboardContext.Root) {
      categoriesText() = posts.map(c ⇒ s"${c.hash}\n${c.text}").mkString("\n")
    }
  }
}