package com.karasiq.nanoboard

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, TextStyle}
import java.time.temporal.ChronoField
import java.time.{ZoneId, ZonedDateTime}
import java.util.Locale

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

object NanoboardMessageGenerator {
  def fromConfig(config: Config) = {
    new NanoboardMessageGenerator(config.getString("client-version"), Try(ZoneId.of(config.getString("default-time-zone"))).getOrElse(ZoneId.systemDefault()))
  }

  def apply(config: Config = ConfigFactory.load()) = {
    fromConfig(config.getConfig("nanoboard"))
  }
}

/**
  * Nanoboard message generator
  * @param clientVersion Client version string
  * @param timeZone Timestamp time zone
  */
class NanoboardMessageGenerator(clientVersion: String, timeZone: ZoneId) {
  /**
    * Message timestamp format
    */
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

  /**
    * Creates new message with timestamp and client header
    * @param parent Parent message hash
    * @param text Message text
    * @return Created message
    */
  def newMessage(parent: String, text: String): NanoboardMessage = {
    val header = s"[g]${timestampFormat.format(ZonedDateTime.now(timeZone))}, client: $clientVersion[/g]"
    NanoboardMessage(parent, s"$header\n$text")
  }
}
