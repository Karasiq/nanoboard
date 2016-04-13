package com.karasiq.nanoboard.server.test

import com.karasiq.nanoboard.encoding.NanoboardCrypto._
import com.karasiq.nanoboard.server.util.FractalMusic
import com.karasiq.nanoboard.utils._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.language.postfixOps

class FractalMusicTest extends FlatSpec with Matchers {
  "Fractal music generator" should "generate WAV from formula" in {
    val result = Await.result(FractalMusic("(t%((t>>3)+((t>>2)%2?-5:-10)/t>>8&(130+((t%65536)>>10))))<<2", 10 minutes), Duration.Inf)
    sha256.digest(result).toHexString() shouldBe "5b19d273699ecb5009bdf505221c4cd05cde3cb8d81b35d34dc0ca19b472f815"
  }

  it should "halt the execution of infinite loop" in {
    intercept[TimeoutException](Await.result(FractalMusic("(function(){while(true){};})()", 1 seconds), Duration.Inf))
  }
}
