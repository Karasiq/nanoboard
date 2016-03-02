package com.karasiq.nanoboard.server.cache

import java.io.Closeable

trait NanoboardCache extends Closeable {
  def +=(url: String): Unit

  def contains(url: String): Boolean
}


