package com.karasiq.nanoboard.streaming

import boopickle.CompositePickler
import boopickle.Default._
import com.karasiq.nanoboard.api.NanoboardMessageData

sealed trait NanoboardEvent

object NanoboardEvent {
  case class PostAdded(post: NanoboardMessageData) extends NanoboardEvent
  case class PostDeleted(hash: String) extends NanoboardEvent
  case class PostVerified(post: NanoboardMessageData) extends NanoboardEvent

  implicit val eventPickler: CompositePickler[NanoboardEvent] = compositePickler[NanoboardEvent]

  eventPickler
    .addConcreteType[PostAdded]
    .addConcreteType[PostDeleted]
    .addConcreteType[PostVerified]
}