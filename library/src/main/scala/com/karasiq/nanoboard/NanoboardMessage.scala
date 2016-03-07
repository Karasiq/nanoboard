package com.karasiq.nanoboard

import java.time._
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, TextStyle}
import java.time.temporal.ChronoField
import java.util.Locale

import com.karasiq.nanoboard.encoding.{DataCipher, DefaultNanoboardMessageFormat}
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Hex

case class NanoboardMessage(parent: String, text: String) {
  def payload: String = parent + text
  def hash: String = Hex.encodeHexString(DataCipher.sha256.digest(payload.getBytes("UTF-8"))).take(32)
}

object NanoboardMessage extends DefaultNanoboardMessageFormat {
  private val clientVersion = ConfigFactory.load().getString("nanoboard.client-version")

  private val timestampFormat = new DateTimeFormatterBuilder()
    .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
    .appendLiteral(", ")
    .appendValue(ChronoField.DAY_OF_MONTH)
    .appendLiteral('/')
    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
    .appendLiteral('/')
    .appendValue(ChronoField.YEAR)
    .appendLiteral(", ")
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .appendLiteral(" (")
    .appendZoneOrOffsetId()
    .appendLiteral(")")
    .toFormatter(Locale.ENGLISH)

  def newMessage(parent: String, text: String): NanoboardMessage = {
    val header = s"[g]${timestampFormat.format(ZonedDateTime.now())}, client: $clientVersion[/g]"
    NanoboardMessage(parent, s"$header\n$text")
  }
}
