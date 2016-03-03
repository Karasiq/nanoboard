import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.server.model._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DatabaseTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val testMessage = NanoboardMessage.newMessage("8b8cfb7574741838450e286909e8fd1f", "Hello world!")
  val db = Database.forConfig("nanoboard.test-database")

  "Database" should "add entry" in {
    println(testMessage)
    assert(testMessage.parent.length == testMessage.hash.length)

    val query = for {
      _ ← DBIO.seq(posts.schema.create, deletedPosts.schema.create, pendingPosts.schema.create, Post.insertMessage(testMessage))
      message ← Post.find(testMessage.hash)
    } yield message

    val result = Await.result(db.run(query), Duration.Inf)
    result shouldBe testMessage

    val answers = Await.result(db.run(Post.thread("8b8cfb7574741838450e286909e8fd1f")), Duration.Inf)
    answers.toVector shouldBe Vector(testMessage → 0)
  }

  it should "delete entry" in {
    val delete = Post.delete(testMessage.hash)
    val query = for (_ ← delete; ps ← posts.result) yield ps
    val result = Await.result(db.run(query), Duration.Inf)
    result shouldBe empty
  }

  override protected def afterAll(): Unit = {
    db.close()
    super.afterAll()
  }
}
