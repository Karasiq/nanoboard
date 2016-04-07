package com.karasiq.nanoboard.encoding

import akka.util.ByteString

import scala.language.implicitConversions

object DataEncodingStage {
  /**
    * Creates sequential data encoder, which is applied one after another in encoding, and backwards in decoding.
    */
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

/**
  * Generic data encoding stage
  */
trait DataEncodingStage {
  /**
    * Encodes data
    * @param data Source data
    * @return Encoded data
    */
  def encode(data: ByteString): ByteString

  /**
    * Decodes encoded data
    * @param data Previously encoded data
    * @return Source data
    */
  def decode(data: ByteString): ByteString
}