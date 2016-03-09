package com.karasiq.nanoboard.frontend.locales

trait BoardLocale {
  def nanoboard: String
  def settings: String
  def containerGeneration: String
  def recentPosts: String
  def recentPostsFrom(post: Int): String
  def categories: String
  def places: String
  def delete: String
  def deleteConfirmation(hash: String): String
  def enqueue: String
  def dequeue: String
  def reply: String
  def insertImage: String
  def submit: String
  def cancel: String
  def pendingPosts: String
  def randomPosts: String
  def imageSize: String
  def imageQuality: String
  def imageFormat: String
  def dataContainer: String
  def generateContainer: String
  def fromTo(from: Int, to: Int): String
  def embeddedImage: String
  def writeYourMessage: String
  def bytes: String
  def postingError: String
  def updateError: String
  def containerGenerationError: String
  def fileNotSelected: String
  def attachmentGenerationError: String
}

object BoardLocale {
  def fromBrowserLanguage(): BoardLocale = {
    import org.scalajs.dom.window.navigator
    navigator.language.toLowerCase match {
      case "ru-ru" | "ru" ⇒
        Russian

      case _ ⇒
        English
    }
  }
}