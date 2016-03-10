package com.karasiq.nanoboard.frontend.api.streaming

import boopickle.Default._
import com.karasiq.nanoboard.frontend.api.NanoboardMessageData

sealed trait NanoboardEvent

object NanoboardEvent {
  case class PostAdded(post: NanoboardMessageData, pending: Boolean) extends NanoboardEvent
  case class PostDeleted(hash: String) extends NanoboardEvent

  implicit val eventPickler = compositePickler[NanoboardEvent]

  eventPickler
    .addConcreteType[PostAdded]
    .addConcreteType[PostDeleted]
}