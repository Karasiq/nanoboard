import com.karasiq.nanoboard.server.util.FractalMusic
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class FractalMusicTest extends FlatSpec with Matchers {
  "Fractal music generator" should "generate WAV from formula" in {
    val result = Await.result(FractalMusic("(t%((t>>3)+((t>>2)%2?-5:-10)/t>>8&(130+((t%65536)>>10))))<<2"), Duration.Inf)
    result.length shouldBe 262188
    result.hashCode() shouldBe -1991558751
  }
}
