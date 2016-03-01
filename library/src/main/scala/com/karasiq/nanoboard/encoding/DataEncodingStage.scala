package com.karasiq.nanoboard.encoding

import akka.util.ByteString

import scala.language.implicitConversions

object DataEncodingStage {
  implicit def stageSeqToStage(seq: Seq[DataEncodingStage]): DataEncodingStage = new DataEncodingStage {
    override def encode(data: ByteString): ByteString = {
      seq.foldLeft(data)((data, stage) ⇒ stage.encode(data))
    }

    override def decode(data: ByteString): ByteString = {
      seq.foldRight(data)((stage, data) ⇒ stage.decode(data))
    }

    override def toString: String = {
      s"Sequential(${seq.mkString(", ")})"
    }
  }
}

trait DataEncodingStage {
  def encode(data: ByteString): ByteString
  def decode(data: ByteString): ByteString
}