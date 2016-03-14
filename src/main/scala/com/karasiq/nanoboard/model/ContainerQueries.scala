package com.karasiq.nanoboard.model

import java.time.Instant

import com.karasiq.nanoboard.api.NanoboardContainer
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext

trait ContainerQueries { self: Tables with PostQueries ⇒
  object Container {
    private def insert(url: String) = containers.forceInsertQuery {
      val exists = (for (c <- containers if c.url === url) yield ()).exists
      val insert = (0L, url, Instant.now().toEpochMilli) <> (DBContainer.tupled, DBContainer.unapply)
      for (container ← Query(insert) if !exists) yield container
    }

    def forUrl(url: String)(implicit ec: ExecutionContext) = {
      for {
        _ ← insert(url)
        id ← containers.filter(_.url === url).map(_.id).result.head
      } yield id
    }

    private def listQuery(offset: ConstColumn[Long], count: ConstColumn[Long]) = {
      containers
        .sortBy(_.time.desc)
        .drop(offset)
        .take(count)
        .map(c ⇒ (c, posts.filter(_.containerId === c.id).length))
    }

    private val listCompiled = Compiled(listQuery _)

    def list(offset: Long, count: Long)(implicit ec: ExecutionContext) = {
      listCompiled(offset, count)
        .result
        .map(_.map {
          case (DBContainer(id, url, time), posts) ⇒
            NanoboardContainer(id, url, time, posts)
        })
    }

    def clearPosts(id: Long)(implicit ec: ExecutionContext) = {
      for {
        hashes ← posts.filter(_.containerId === id).map(_.hash).result
        deleted ← DBIO.sequence(hashes.map(Post.delete))
      } yield deleted.flatten
    }
  }
}
