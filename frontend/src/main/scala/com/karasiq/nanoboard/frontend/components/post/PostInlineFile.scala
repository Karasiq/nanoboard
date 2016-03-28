package com.karasiq.nanoboard.frontend.components.post

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.{Bootstrap, BootstrapHtmlComponent}
import com.karasiq.nanoboard.frontend.NanoboardController
import com.karasiq.nanoboard.frontend.utils.Blobs
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._

private[components] object PostInlineFile {
  def apply(fileName: String, base64: String, fileType: String)(implicit ctx: Ctx.Owner, controller: NanoboardController): PostInlineFile = {
    new PostInlineFile(fileName, base64, fileType)
  }
}

private[components] final class PostInlineFile(val fileName: String, val base64: String, val fileType: String)(implicit ctx: Ctx.Owner, controller: NanoboardController) extends BootstrapHtmlComponent[dom.html.Anchor] {
  val file = Blobs.asBlob(base64, fileType)

  override def renderTag(md: Modifier*): RenderedTag = {
    a(fontWeight.bold, href := "#", "file-archive-o".fontAwesome(FontAwesome.fixedWidth), fileName, onclick := Bootstrap.jsClick { _ ⇒
      Blobs.saveBlob(file, fileName)
    })
  }
}
