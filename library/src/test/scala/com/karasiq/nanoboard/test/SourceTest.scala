package com.karasiq.nanoboard.test

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatest.tags.Network

import com.karasiq.nanoboard.sources.png.UrlPngSource

@Network
class SourceTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val actorSystem = ActorSystem("source-test")
  implicit val actorMaterializer = ActorMaterializer()
  val source = UrlPngSource()

  "Source loader" should "load messages from image" in {
    val imageSource = source.messagesFromImage("https://endchan.xyz/.media/78db64519e62d9edce7b6a8dbb47fd17-imagepng.png")
    val messages = Await.result(imageSource.runWith(Sink.seq), Duration.Inf)
    messages.length shouldBe 29
  }

  it should "load messages from thread" in {
    val imageSource = source.imagesFromPage("https://endchan.xyz/test/res/971.html").flatMapConcat(source.messagesFromImage)
    val messages = Await.result(imageSource.take(100).runWith(Sink.seq), Duration.Inf)
    messages.length shouldBe 100
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }
}
