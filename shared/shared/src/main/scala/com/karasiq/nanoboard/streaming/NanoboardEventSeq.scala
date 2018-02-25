package com.karasiq.nanoboard.streaming

import boopickle.Default._

final case class NanoboardEventSeq(events: Seq[NanoboardEvent])

object NanoboardEventSeq {
  implicit val nanoboardEventSeqFormat = generatePickler[NanoboardEventSeq]
}