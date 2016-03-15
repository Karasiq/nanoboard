package com.karasiq.nanoboard.streaming

import boopickle.Default._

sealed trait NanoboardSubscription
object NanoboardSubscription {
  case object Unfiltered extends NanoboardSubscription
  case class PostHashes(hashes: Set[String]) extends NanoboardSubscription

  implicit val subscriptionPickler = compositePickler[NanoboardSubscription]

  subscriptionPickler.addConcreteType[PostHashes]
  subscriptionPickler.addConcreteType[Unfiltered.type]
}