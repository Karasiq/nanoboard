package com.karasiq.nanoboard.server

import java.time.Instant

import com.karasiq.nanoboard.dispatcher.NanoboardMessageData
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

package object model {
  case class DBPost(hash: String, parent: String, message: String, firstSeen: Long) {
    def asThread(answers: Int): NanoboardMessageData = {
      NanoboardMessageData(Some(parent), hash, message, answers)
    }
  }
  class Post(tag: Tag) extends Table[DBPost](tag, "posts") {
    def hash = column[String]("hash", O.SqlType("char(32)"), O.PrimaryKey)
    def parent = column[String]("parent_hash", O.SqlType("char(32)"))
    def message = column[String]("message")
    def firstSeen = column[Long]("first_seen")

    def threadIdx = index("thread_index", parent, unique = false)
    def recentIdx = index("recent_index", firstSeen, unique = false)
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

    def recent(offset: Int, count: Int)(implicit ec: ExecutionContext) = {
      posts
        .sortBy(_.firstSeen.desc)
        .drop(offset)
        .take(count)
        .map(post ⇒ (post, posts.filter(_.parent === post.hash).length))
        .result
        .map(_.map {
          case (post, answers) ⇒
            post.asThread(answers)
        })
    }

    def pending(offset: Int, count: Int)(implicit ec: ExecutionContext) = {
      pendingPosts
        .flatMap(_.post)
        .sortBy(_.firstSeen.asc)
        .drop(offset)
        .take(count)
        .result
        .map(_.map(_.asThread(0)))
    }

    def thread(hash: String, offset: Int, count: Int)(implicit ec: ExecutionContext) = {
      val query = posts.filter(_.parent === hash)
        .sortBy(_.firstSeen.desc)
        .drop(offset)
        .take(count)

      def withAnswerCount(query: Query[Post, DBPost, Seq]) = query.map { post ⇒
        (post, posts.filter(_.parent === post.hash).length)
      }

      for {
        originalPost ← withAnswerCount(posts.filter(_.hash === hash)).result
        answers ← withAnswerCount(query).result
      } yield (originalPost ++ answers).map {
        case (post, answersCount) ⇒
          post.asThread(answersCount)
      }
    }

    def get(hash: String)(implicit ec: ExecutionContext) = {
      thread(hash, 0, 0).map(_.headOption)
    }

    def delete(hash: String)(implicit ec: ExecutionContext) = {
      def deleteCascade(hash: String): DBIOAction[Unit, NoStream, Effect.Write with Effect.Read] = {
        val query = posts.filter(_.parent === hash)
        DBIO.seq(
          query.map(_.hash).result.flatMap(ps ⇒ DBIO.sequence[Unit, Seq, Effect.Write with Effect.Read](ps.map(deleteCascade))),
          query.delete,
          pendingPosts.filter(_.hash === hash).delete,
          Category.delete(hash),
          deletedPosts.insertOrUpdate(hash),
          posts.filter(_.hash === hash).delete
        )
      }

      deleteCascade(hash)
    }
  }

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
