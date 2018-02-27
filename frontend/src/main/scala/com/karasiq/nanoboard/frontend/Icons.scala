package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.Bootstrap.default._

/**
  * Nanoboard application icons
  */
object Icons {
  @inline 
  private[this] def fa(name: String): IconModifier = name.fontAwesome(FontAwesome.fixedWidth)

  lazy val thread = fa("server")
  lazy val settings = fa("cogs")
  lazy val container = fa("globe")
  lazy val previous = fa("angle-double-left")
  lazy val next = fa("angle-double-right")
  lazy val removeContainer = fa("ban")
  lazy val batchDelete = fa("eraser")
  lazy val clearDeleted = fa("eye-slash")
  lazy val preferences = fa("wrench")
  lazy val control = fa("warning")
  lazy val containers = fa("archive")
  lazy val answers = fa("envelope-o")
  lazy val recent = fa("newspaper-o")
  lazy val categories = fa("sitemap")
  lazy val link = fa("link")
  lazy val parent = fa("level-up")
  lazy val delete = fa("trash-o")
  lazy val enqueue = fa("sign-in")
  lazy val dequeue = fa("sign-out")
  lazy val image = fa("file-image-o")
  lazy val video = fa("play-circle")
  lazy val file = fa("file-archive-o")
  lazy val submit = fa("mail-forward")
  lazy val reply = fa("reply")
  lazy val source = fa("file-text-o")
  lazy val music = fa("music")
  lazy val verify = fa("key")
}
