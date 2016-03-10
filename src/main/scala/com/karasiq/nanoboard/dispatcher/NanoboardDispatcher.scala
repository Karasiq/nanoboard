package com.karasiq.nanoboard.dispatcher

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardCategory
import com.karasiq.nanoboard.model.NanoboardMessageData

import scala.concurrent.Future

trait NanoboardDispatcher {
  def createContainer(pending: Int, random: Int, format: String, container: ByteString): Future[ByteString]
  def recent(offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def pending(offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def places(): Future[Seq[String]]
  def categories(): Future[Seq[NanoboardMessageData]]
  def post(hash: String): Future[Option[NanoboardMessageData]]
  def thread(hash: String, offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def reply(parent: String, text: String): Future[NanoboardMessageData]
  def markAsNotPending(message: String): Future[Unit]
  def markAsPending(message: String): Future[Unit]
  def delete(message: String): Future[Seq[String]]
  def updatePlaces(places: Seq[String]): Future[Unit]
  def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit]
}
