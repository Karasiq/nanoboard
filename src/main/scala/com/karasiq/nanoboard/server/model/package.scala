package com.karasiq.nanoboard.server

import java.time.Instant

import com.karasiq.nanoboard.encoding.NanoboardMessage
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

package object model {
  case class DBPost(hash: String, parent: String, message: String, firstSeen: Long)
  class Post(tag: Tag) extends Table[DBPost](tag, "posts") {
    def hash = column[String]("hash", O.SqlType("char(32)"), O.PrimaryKey)
    def parent = column[String]("parent_hash", O.SqlType("char(32)"))
    def message = column[String]("message")
    def firstSeen = column[Long]("first_seen")

    def idx = index("post_index", parent, unique = false)
    def * = (hash, parent, message, firstSeen) <> (DBPost.tupled, DBPost.unapply)
  }

  val posts = TableQuery[Post]

  class DeletedPost(tag: Tag) extends Table[String](tag, "posts_deleted") {
    def hash = column[String]("hash", O.PrimaryKey)
    def * = hash
  }

  val deletedPosts = TableQuery[DeletedPost]

  object Post {
    def insertMessage(m: NanoboardMessage) = posts.forceInsertQuery {
      val deleted = (for (p <- deletedPosts if p.hash === m.hash) yield ()).exists
      val exists = (for (p <- posts if p.hash === m.hash) yield ()).exists
      val insert = (m.hash, m.parent, m.text, Instant.now().toEpochMilli) <> (DBPost.tupled, DBPost.unapply)
      for (message <- Query(insert) if !deleted && !exists) yield message
    }

    def insertMessages(messages: Seq[NanoboardMessage]) = {
      DBIO.sequence(messages.map(insertMessage))
    }

    def answers(hash: String)(implicit ec: ExecutionContext) = {
      val query = posts
        .filter(_.parent === hash)
        .sortBy(_.firstSeen)
        .map(_.message)

      query.result.map(_.map(NanoboardMessage(hash, _)))
    }

    def find(hash: String)(implicit ec: ExecutionContext) = {
      val query = for(post ← posts if post.hash === hash) yield (post.parent, post.message)
      for((parent, message) ← query.result.head) yield NanoboardMessage(parent, message)
    }

    def delete(hash: String) = {
      DBIO.seq(
        posts.filter(_.hash === hash).delete,
        deletedPosts += hash
      )
    }
  }
}
