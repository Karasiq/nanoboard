package com.karasiq.nanoboard.frontend.components.post

import scalatags.JsDom.all._

import com.karasiq.bootstrap.Bootstrap.default._
import com.karasiq.nanoboard.frontend.{Icons, NanoboardController}
import com.karasiq.nanoboard.frontend.utils.Blobs

private[components] object PostInlineFile {
  def apply(fileName: String, base64: String, fileType: String)(implicit controller: NanoboardController): PostInlineFile = {
    new PostInlineFile(fileName, base64, fileType)
  }
}

private[components] final class PostInlineFile(val fileName: String, val base64: String, val fileType: String)
                                              (implicit controller: NanoboardController) extends BootstrapHtmlComponent {
  val file = Blobs.fromBase64(base64, fileType)

  override def renderTag(md: Modifier*): TagT = {
    a(fontWeight.bold, href := "#", Icons.file, fileName, onclick := Callback.onClick { _ ⇒
      Blobs.saveBlob(file, fileName)
    })
  }
}
