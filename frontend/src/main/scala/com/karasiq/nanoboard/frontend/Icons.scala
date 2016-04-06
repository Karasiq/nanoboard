package com.karasiq.nanoboard.frontend

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.icons.{FontAwesome, IconModifier}

/**
  * Nanoboard application icons
  */
object Icons {
  private def fa(name: String): IconModifier = name.fontAwesome(FontAwesome.fixedWidth)

  def thread = fa("server")
  def settings = fa("cogs")
  def container = fa("globe")
  def previous = fa("angle-double-left")
  def next = fa("angle-double-right")
  def removeContainer = fa("ban")
  def batchDelete = fa("eraser")
  def clearDeleted = fa("eye-slash")
  def preferences = fa("wrench")
  def control = fa("warning")
  def containers = fa("archive")
  def answers = fa("envelope-o")
  def recent = fa("newspaper-o")
  def categories = fa("sitemap")
  def link = fa("link")
  def parent = fa("level-up")
  def delete = fa("trash-o")
  def enqueue = fa("sign-in")
  def dequeue = fa("sign-out")
  def image = fa("file-image-o")
  def video = fa("play-circle")
  def file = fa("file-archive-o")
  def submit = fa("mail-forward")
  def reply = fa("reply")
  def source = fa("file-text-o")
  def music = fa("music")
  def verify = fa("key")
}
