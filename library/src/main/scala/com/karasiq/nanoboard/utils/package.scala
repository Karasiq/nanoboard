package com.karasiq.nanoboard

import akka.util.ByteString
import org.apache.commons.codec.binary.Hex

package object utils {
  private[nanoboard] implicit class ByteStringCompanionOps(private val bs: ByteString.type) extends AnyVal {
    /**
      * Creates byte string from hex string (each byte represented as two chars from 00 to FF)
      * @param string Hex string
      * @return Byte string
      * @see [[org.apache.commons.codec.binary.Hex#decodeHex(char[])]]
      */
    def fromHexString(string: String): ByteString = {
      bs.apply(Hex.decodeHex(string.toCharArray))
    }
  }

  private[nanoboard] implicit class ByteStringOps(private val bs: ByteString) extends AnyVal {
    /**
      * Encodes byte string as hex string (each byte represented as two chars from 00 to FF)
      * @param lowerCase Use lower-case hex chars
      * @return Hex string
      * @see [[org.apache.commons.codec.binary.Hex#encodeHex(byte[], boolean)]]
      */
    def toHexString(lowerCase: Boolean = true): String = {
      String.valueOf(Hex.encodeHex(bs.toArray, lowerCase))
    }
  }
}
