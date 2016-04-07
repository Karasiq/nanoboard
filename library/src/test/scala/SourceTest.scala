import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.karasiq.nanoboard.sources.png.UrlPngSource
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class SourceTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val actorSystem = ActorSystem("source-test")
  implicit val actorMaterializer = ActorMaterializer()
  val source = UrlPngSource()

  ignore should "load messages from image" in {
    val imageSource = source.messagesFromImage("http://dobrochan.com/src/png/1512/00cfb029473e4a90.png")
    val messages = Await.result(imageSource.runWith(Sink.seq), Duration.Inf)
    messages.length shouldBe 72
  }

  ignore should "load messages from thread" in {
    val imageSource = source.imagesFromPage("http://dobrochan.com/slow/res/26779.xhtml").flatMapConcat(source.messagesFromImage)
    val messages = Await.result(imageSource.take(100).runWith(Sink.seq), Duration.Inf)
    messages.length shouldBe 100
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }
}
