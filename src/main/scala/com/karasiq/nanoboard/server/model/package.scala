package com.karasiq.nanoboard.server

import java.time.Instant

import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
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

  class PendingPost(tag: Tag) extends Table[String](tag, "pending_posts") {
    def hash = column[String]("hash", O.SqlType("char(32)"), O.PrimaryKey)
    def post = foreignKey("post_fk", hash, posts)(_.hash, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    def * = hash
  }

  val pendingPosts = TableQuery[PendingPost]

  class Place(tag: Tag) extends Table[String](tag, "places") {
    def url = column[String]("place_url", O.PrimaryKey)
    def * = url
  }

  val places = TableQuery[Place]

  class Category(tag: Tag) extends Table[NanoboardCategory](tag, "categories") {
    def hash = column[String]("category_hash", O.SqlType("char(32)"), O.PrimaryKey)
    def name = column[String]("category_name")
    def * = (hash, name) <> (NanoboardCategory.tupled, NanoboardCategory.unapply)
  }

  val categories = TableQuery[Category]

  object Post {
    def addReply(m: NanoboardMessage) = DBIO.seq(insertMessage(m), pendingPosts.forceInsertQuery {
      val exists = (for (p <- pendingPosts if p.hash === m.hash) yield ()).exists
      for (message <- Query(m.hash) if !exists) yield message
    }, deletedPosts.filter(_.hash === m.hash).delete)

    def insertMessage(m: NanoboardMessage) = posts.forceInsertQuery {
      val deleted = (for (p <- deletedPosts if p.hash === m.hash) yield ()).exists
      val exists = (for (p <- posts if p.hash === m.hash) yield ()).exists
      val insert = (m.hash, m.parent, m.text, Instant.now().toEpochMilli) <> (DBPost.tupled, DBPost.unapply)
      for (message <- Query(insert) if !deleted && !exists) yield message
    }

    def insertMessages(messages: Seq[NanoboardMessage]) = {
      DBIO.sequence(messages.map(insertMessage))
    }

    def thread(hash: String)(implicit ec: ExecutionContext) = {
      val query = posts.filter(_.parent === hash).sortBy(_.firstSeen.desc)

      def withAnswerCount(query: Query[Post, DBPost, Seq]) = query.map { post ⇒
        (post, posts.filter(_.parent === post.hash).length)
      }

      for {
        originalPost ← withAnswerCount(posts.filter(_.hash === hash)).result
        answers ← withAnswerCount(query).result
      } yield (originalPost ++ answers).map {
        case (post, answersCount) ⇒
          NanoboardMessage(post.parent, post.message) → answersCount
      }
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

  object Place {
    def list() = {
      places.result
    }

    def add(url: String) = {
      places.insertOrUpdate(url)
    }

    def delete(url: String) = {
      places.filter(_.url === url).delete
    }
  }

  object Category {
    def list()(implicit ec: ExecutionContext) = {
      val query = categories.map { c ⇒
        (c.hash, c.name, posts.filter(_.parent === c.hash).length)
      }
      query.result.map(_.map {
        case (hash, name, answers) ⇒
          NanoboardCategory(hash, name) → answers
      })
    }

    def add(hash: String, name: String) = {
      categories.insertOrUpdate(NanoboardCategory(hash, name))
    }

    def delete(hash: String) = {
      categories.filter(_.hash === hash).delete
    }
  }
}
