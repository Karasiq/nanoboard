package com.karasiq.nanoboard.dispatcher

import com.karasiq.nanoboard.NanoboardCategory

import scala.concurrent.Future

case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

trait NanoboardDispatcher {
  def places(): Future[Seq[String]]
  def categories(): Future[Seq[NanoboardMessageData]]
  def get(thread: String, offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def reply(parent: String, text: String): Future[NanoboardMessageData]
  def delete(message: String): Future[Unit]
  def updatePlaces(places: Seq[String]): Future[Unit]
  def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit]
}
