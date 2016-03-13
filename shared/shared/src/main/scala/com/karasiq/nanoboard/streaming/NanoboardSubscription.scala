package com.karasiq.nanoboard.streaming

import boopickle.Default._

sealed trait NanoboardSubscription
object NanoboardSubscription {
  case object Unfiltered extends NanoboardSubscription
  case class PostHashes(hashes: Set[String]) extends NanoboardSubscription

  implicit val subscriptionPickler: Pickler[NanoboardSubscription] = new Pickler[NanoboardSubscription] {
    def pickle(obj: NanoboardSubscription)(implicit state: PickleState) = obj match {
      case Unfiltered ⇒
        state.pickle(0)

      case PostHashes(hashes) ⇒
        state.pickle(1)
        state.pickle(hashes)
    }

    def unpickle(implicit state: UnpickleState) = {
      if (state.unpickle[Int] == 1) {
        PostHashes(state.unpickle[Set[String]])
      } else {
        Unfiltered
      }
    }
  }
}