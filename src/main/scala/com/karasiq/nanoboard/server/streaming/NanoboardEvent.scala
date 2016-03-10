package com.karasiq.nanoboard.server.streaming

import boopickle.Default._
import com.karasiq.nanoboard.model.NanoboardMessageData

sealed trait NanoboardEvent

object NanoboardEvent {
  case class PostAdded(post: NanoboardMessageData, pending: Boolean = false) extends NanoboardEvent
  case class PostDeleted(hash: String) extends NanoboardEvent

  implicit val eventPickler = compositePickler[NanoboardEvent]

  eventPickler
    .addConcreteType[PostAdded]
    .addConcreteType[PostDeleted]
}