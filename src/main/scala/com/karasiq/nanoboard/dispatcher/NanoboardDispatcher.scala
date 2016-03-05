package com.karasiq.nanoboard.dispatcher

import akka.util.ByteString
import com.karasiq.nanoboard.NanoboardCategory

import scala.concurrent.Future

case class NanoboardMessageData(parent: Option[String], hash: String, text: String, answers: Int)

trait NanoboardDispatcher {
  def createContainer(pending: Int, random: Int, format: String, container: ByteString): Future[ByteString]
  def recent(offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def pending(offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def places(): Future[Seq[String]]
  def categories(): Future[Seq[NanoboardMessageData]]
  def post(hash: String): Future[Option[NanoboardMessageData]]
  def thread(hash: String, offset: Int, count: Int): Future[Seq[NanoboardMessageData]]
  def reply(parent: String, text: String): Future[NanoboardMessageData]
  def delete(message: String): Future[Unit]
  def updatePlaces(places: Seq[String]): Future[Unit]
  def updateCategories(categories: Seq[NanoboardCategory]): Future[Unit]
}
