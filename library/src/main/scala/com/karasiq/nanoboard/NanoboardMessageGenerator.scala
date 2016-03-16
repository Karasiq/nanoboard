package com.karasiq.nanoboard

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, TextStyle}
import java.time.temporal.ChronoField
import java.time.{ZoneId, ZonedDateTime}
import java.util.Locale

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

object NanoboardMessageGenerator {
  def apply(config: Config = ConfigFactory.load()) = {
    new NanoboardMessageGenerator(config.getString("nanoboard.client-version"), Try(ZoneId.of(config.getString("nanoboard.default-time-zone"))).getOrElse(ZoneId.systemDefault()))
  }
}

class NanoboardMessageGenerator(clientVersion: String, defaultTimeZone: ZoneId) {
  protected val timestampFormat = new DateTimeFormatterBuilder()
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
    val header = s"[g]${timestampFormat.format(ZonedDateTime.now(defaultTimeZone))}, client: $clientVersion[/g]"
    NanoboardMessage(parent, s"$header\n$text")
  }
}
