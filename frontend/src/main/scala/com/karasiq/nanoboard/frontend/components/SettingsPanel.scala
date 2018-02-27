package com.karasiq.nanoboard.frontend.components

import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.api.NanoboardCategory
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout

object SettingsPanel {
  def apply()(implicit controller: NanoboardController): SettingsPanel = {
    new SettingsPanel
  }
}

final class SettingsPanel(implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  import controller.{locale, style}

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
      assert(categories.forall(c ⇒ c.hash.matches("[a-fA-F0-9]{32}") && c.name.nonEmpty))
      categories
    }.getOrElse(Vector.empty)
  }

  private val loading = Var(false)

  private val buttonDisabled = Rx {
    loading() || categories().isEmpty || places().isEmpty
  }

  override def renderTag(md: Modifier*) = {
    val batchDelete = {
      val offset, count = Var("0")
      val loading = Var(false)
      val disabled = Rx {
        loading() || Try(offset().toInt).filter(_ >= 0).isFailure || Try(count().toInt).filter(_ > 0).isFailure
      }
      Form(
        FormInput.number(locale.offset, style.input, min := 0, offset.reactiveInput),
        FormInput.number(locale.count, style.input, min := 0, count.reactiveInput),
        Button(ButtonStyle.danger, block = true)(Icons.batchDelete, locale.batchDelete, "disabled".classIf(disabled), onclick := Callback.onClick { _ ⇒
          if (!disabled.now) {
            Notifications.confirmation(locale.batchDeleteConfirmation(count.now.toInt), Layout.topLeft) {
              loading() = true
              NanoboardApi.delete(offset.now.toInt, count.now.toInt).onComplete {
                case Success(hashes) ⇒
                  controller.updatePosts()
                  controller.updateCategories()
                  count() = ""
                  loading() = false
                  Notifications.success(locale.batchDeleteSuccess(hashes.length), Layout.topRight)

                case Failure(exc) ⇒
                  loading() = false
                  Notifications.error(exc)(locale.batchDeleteError, Layout.topRight)
              }
            }
          }
        })
      )
    }

    val clearDeleted = {
      val loading = Var(false)
      Button(ButtonStyle.warning, block = true)(Icons.clearDeleted, locale.clearDeleted, "disabled".classIf(loading), onclick := Callback.onClick { _ ⇒
        if (!loading.now) {
          Notifications.confirmation(locale.clearDeletedConfirmation, Layout.topLeft) {
            loading() = true
            NanoboardApi.clearDeleted().onComplete {
              case Success(count) ⇒
                loading() = false
                Notifications.success(locale.clearDeletedSuccess(count), Layout.topRight)

              case Failure(exc) ⇒
                loading() = false
                Notifications.error(exc)(locale.clearDeletedError, Layout.topRight)
            }
          }
        }
      })
    }

    val navigation = Navigation.pills(
      NavigationTab(locale.preferences, "server", Icons.preferences, div(
        GridSystem.mkRow(Form(
          FormInput.textArea(locale.places, style.input, rows := 15, placesText.reactiveInput)("has-error".classIf(places.map(_.isEmpty))),
          FormInput.textArea(locale.categories, style.input, rows := 15, categoriesText.reactiveInput)("has-error".classIf(categories.map(_.isEmpty)))
        )),
        GridSystem.mkRow(Button(block = true)(locale.submit, style.submit, "disabled".classIf(buttonDisabled), onclick := Callback.onClick { _ ⇒
          if (!buttonDisabled.now) {
            loading() = true
            Future.sequence(Seq(NanoboardApi.setCategories(categories.now), NanoboardApi.setPlaces(places.now))).onComplete {
              case Success(_) ⇒
                loading() = false
                controller.updateCategories()

              case Failure(exc) ⇒
                Notifications.error(exc)(locale.settingsUpdateError, Layout.topRight)
                loading() = false
            }
          }
        }))
      )),
      NavigationTab(locale.control, "control", Icons.control, div(
        GridSystem.mkRow(h3(locale.batchDelete)),
        GridSystem.mkRow(batchDelete),
        GridSystem.mkRow(h3(locale.clearDeleted)),
        GridSystem.mkRow(clearDeleted)
      )),
      NavigationTab(locale.containers, "containers", Icons.containers, div(
        ContainersPanel(30)
      ))
    )

    div(
      navigation,
      marginBottom := 50.px
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
}
