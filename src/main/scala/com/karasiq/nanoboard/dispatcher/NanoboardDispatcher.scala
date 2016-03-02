package com.karasiq.nanoboard.dispatcher

import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.concurrent.Future

trait NanoboardDispatcher {
  def categories(): Future[Seq[(NanoboardCategory, Int)]]
  def get(thread: String): Future[Seq[(NanoboardMessage, Int)]]
  def reply(parent: String, text: String): Future[NanoboardMessage]
  def delete(message: String): Future[Unit]
}
