package com.karasiq.nanoboard.model

import akka.util.ByteString
import com.karasiq.nanoboard.{NanoboardCategory, NanoboardMessage}
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.language.postfixOps

trait Tables {
  implicit val byteStringColumnType = MappedColumnType.base[ByteString, Array[Byte]](_.toArray[Byte], ByteString.apply)

  case class DBPost(hash: String, parent: String, text: String, firstSeen: Long, containerId: Long, pow: ByteString, signature: ByteString)

  // TODO: Descending recent index: https://github.com/slick/slick/issues/1035
  class Post(tag: Tag) extends Table[DBPost](tag, "posts") {
    def hash = column[String]("hash", O.SqlType(s"char(${NanoboardMessage.HASH_LENGTH})"), O.PrimaryKey)
    def parent = column[String]("parent_hash", O.SqlType(s"char(${NanoboardMessage.HASH_LENGTH})"))
    def text = column[String]("message")
    def firstSeen = column[Long]("first_seen")
    def containerId = column[Long]("container_id")
    def pow = column[ByteString]("pow_value", O.SqlType(s"binary(${NanoboardMessage.POW_LENGTH})"))
    def signature = column[ByteString]("signature", O.SqlType(s"binary(${NanoboardMessage.SIGNATURE_LENGTH})"))

    def container = foreignKey("post_container", containerId, containers)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
    def threadIdx = index("thread_index", (parent, firstSeen), unique = false)
    def recentIdx = index("recent_index", firstSeen, unique = false)
    def containerIdx = index("container_index", containerId, unique = false)
    def * = (hash, parent, text, firstSeen, containerId, pow, signature) <> (DBPost.tupled, DBPost.unapply)
  }

  val posts = TableQuery[Post]

  class DeletedPost(tag: Tag) extends Table[String](tag, "posts_deleted") {
    def hash = column[String]("hash", O.SqlType(s"char(${NanoboardMessage.HASH_LENGTH})"), O.PrimaryKey)
    def * = hash
  }

  val deletedPosts = TableQuery[DeletedPost]

  class PendingPost(tag: Tag) extends Table[String](tag, "pending_posts") {
    def hash = column[String]("hash", O.SqlType(s"char(${NanoboardMessage.HASH_LENGTH})"), O.PrimaryKey)
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
    def hash = column[String]("category_hash", O.SqlType(s"char(${NanoboardMessage.HASH_LENGTH})"), O.PrimaryKey)
    def name = column[String]("category_name")
    def * = (hash, name) <> (NanoboardCategory.tupled, NanoboardCategory.unapply)
  }

  val categories = TableQuery[Category]

  case class DBContainer(id: Long, url: String, time: Long)
  class Container(tag: Tag) extends Table[DBContainer](tag, "containers") {
    def id = column[Long]("container_id", O.PrimaryKey, O.AutoInc)
    def url = column[String]("container_url")
    def time = column[Long]("container_time")

    def idx = index("container_url_index", url, unique = true)
    def timeIdx = index("container_time_index", time)
    def * = (id, url, time) <> (DBContainer.tupled, DBContainer.unapply)
  }

  val containers = TableQuery[Container]
}
