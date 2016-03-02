package com.karasiq.nanoboard.server.cache

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

final class MapDbNanoboardCache(config: Config) extends NanoboardCache {
  import com.karasiq.mapdb.{MapDbSingleFileProducer, MapDbWrapper}
  import org.mapdb.DBMaker.Maker
  import org.mapdb.Serializer

  private object DbProvider extends MapDbSingleFileProducer(Paths.get(config.getString("path"))) {
    override protected def setSettings(dbMaker: Maker): Maker = {
      dbMaker
        .transactionDisable()
        .compressionEnable()
        .executorEnable()
        .asyncWriteEnable()
        .asyncWriteFlushDelay(1000)
        .cacheSoftRefEnable()
    }
  }

  private val db = DbProvider()

  private val cache = MapDbWrapper(db).createHashSet[String]("url_cache")(_
    .serializer(Serializer.STRING_XXHASH)
    .expireAfterAccess(7, TimeUnit.DAYS)
  )

  def +=(url: String): Unit = {
    cache += url
  }

  def contains(url: String): Boolean = {
    cache.contains(url)
  }

  override def close(): Unit = {
    DbProvider.close()
  }
}
