package com.karasiq.nanoboard.frontend.locales

import com.karasiq.nanoboard.api.NanoboardContainer

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
  def imageScale: String
  def imageSize: String
  def imageQuality: String
  def imageFormat: String
  def imageSharpness: String
  def useServerRendering: String
  def preview: String
  def dataContainer: String
  def generateContainer: String
  def fromTo(from: Int, to: Int): String
  def embeddedImage: String
  def writeYourMessage: String
  def bytes: String
  def style: String
  def preferences: String
  def control: String
  def offset: String
  def count: String
  def batchDelete: String
  def batchDeleteConfirmation(count: Int): String
  def batchDeleteSuccess(count: Int): String
  def clearDeleted: String
  def clearDeletedConfirmation: String
  def clearDeletedSuccess(count: Int): String
  def containers: String
  def container(c: NanoboardContainer): String
  def file: String
  def source: String
  def verify: String
  def verificationError: String
  def clearDeletedError: String
  def postingError: String
  def updateError: String
  def containerGenerationError: String
  def fileNotSelected: String
  def attachmentGenerationError: String
  def settingsUpdateError: String
  def batchDeleteError: String
  def webSocketError: String
}

object BoardLocale {
  def browserLanguage: String = {
    import org.scalajs.dom.window.navigator
    navigator.language
  }

  def fromBrowserLanguage(): BoardLocale = {
    browserLanguage.toLowerCase match {
      case "ru-ru" | "ru" ⇒
        Russian

      case _ ⇒
        English
    }
  }
}