package com.karasiq.nanoboard.model

import com.karasiq.nanoboard.NanoboardCategory
import com.karasiq.nanoboard.api.NanoboardMessageData
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext

trait ConfigQueries { self: Tables ⇒
  object Place {
    def list() = {
      places.result
    }

    def update(newList: Seq[String])(implicit ec: ExecutionContext) = DBIO.sequence(
      newList.map(url ⇒ places.insertOrUpdate(url)) :+
        places.filterNot(_.url inSet newList).delete
    ).map(_ ⇒ ())
  }

  object Category {
    def list()(implicit ec: ExecutionContext) = {
      val query = categories.map { c ⇒
        (c.hash, c.name, posts.filter(_.parent === c.hash).length)
      }
      query.result.map(_.map {
        case (hash, name, answers) ⇒
          NanoboardMessageData(None, hash, name, answers)
      })
    }

    def update(newList: Seq[NanoboardCategory])(implicit ec: ExecutionContext) = DBIO.sequence(
      newList.map(c ⇒ add(c.hash, c.name)):+
        categories.filterNot(_.hash inSet newList.map(_.hash)).delete
    ).map(_ ⇒ ())

    def add(hash: String, name: String) = DBIO.seq(
      deletedPosts.filter(_.hash === hash).delete,
      categories.insertOrUpdate(NanoboardCategory(hash, name))
    )

    def delete(hash: String) = {
      categories.filter(_.hash === hash).delete
    }
  }
}
