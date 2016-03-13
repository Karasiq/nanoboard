package com.karasiq.nanoboard.model

import com.karasiq.nanoboard.NanoboardCategory
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.language.postfixOps

trait Tables {
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

    def threadIdx = index("thread_index", (parent, firstSeen), unique = false)
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
}
