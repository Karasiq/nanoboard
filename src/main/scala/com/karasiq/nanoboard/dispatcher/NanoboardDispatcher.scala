package com.karasiq.nanoboard.dispatcher

import akka.util.ByteString
import com.karasiq.nanoboard.api.{NanoboardContainer, NanoboardMessageData}
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}

import scala.concurrent.Future

trait NanoboardDispatcher {
  def createContainer(pending: Int, random: Int, format: String, container: ByteString): Future[ByteString]
  def recent(offset: Long, count: Long): Future[Seq[NanoboardMessageData]]
  def pending(offset: Long, count: Long): Future[Seq[NanoboardMessageData]]
  def places(): Future[Seq[String]]
  def categories(): Future[Seq[NanoboardMessageData]]
  def post(hash: String): Future[Option[NanoboardMessageData]]
  def thread(hash: String, offset: Long, count: Long): Future[Seq[NanoboardMessageData]]
  def addPost(source: String, message: NanoboardMessage): Future[Int]
  def reply(parent: String, text: String): Future[NanoboardMessageData]
  def markAsNotPending(message: String): Future[Unit]
  def markAsPending(message: String): Future[Unit]
  def delete(hash: String): Future[Seq[String]]
  def delete(offset: Long, count: Long): Future[Seq[String]]
  def clearDeleted(): Future[Int]
  def updatePlaces(places: Seq[String]): Future[Unit]
  def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit]
  def containers(offset: Long, count: Long): Future[Seq[NanoboardContainer]]
  def clearContainer(id: Long): Future[Seq[String]]
}
