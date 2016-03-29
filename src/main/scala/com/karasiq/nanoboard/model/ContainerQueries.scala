package com.karasiq.nanoboard.model

import java.time.Instant

import com.karasiq.nanoboard.api.NanoboardContainer
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext

trait ContainerQueries { self: Tables with PostQueries ⇒
  object Container {
    def create(url: String)(implicit ec: ExecutionContext) = {
      containers.returning(containers.map(_.id)) += DBContainer(0, url, Instant.now().toEpochMilli)
    }

    def forUrl(url: String)(implicit ec: ExecutionContext) = {
      containers.filter(_.url === url).map(_.id).result.headOption.flatMap {
        case Some(id) ⇒
          DBIO.successful(id)

        case None ⇒
          create(url)
      }
    }

    private def listQuery(offset: ConstColumn[Long], count: ConstColumn[Long]) = {
      containers
        .map(c ⇒ (c, posts.filter(_.containerId === c.id).length))
        .filter(_._2 > 0)
        .sortBy(_._1.time.desc)
        .drop(offset)
        .take(count)
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
      } yield deleted
    }
  }
}
