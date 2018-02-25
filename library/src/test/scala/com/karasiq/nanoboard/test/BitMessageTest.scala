package com.karasiq.nanoboard.test

import org.scalatest.{FlatSpec, Matchers}

import com.karasiq.nanoboard.NanoboardMessage
import com.karasiq.nanoboard.sources.bitmessage.BitMessageTransport

class BitMessageTest extends FlatSpec with Matchers {
  "BitMessage transport" should "encode message" in {
    val message = NanoboardMessage("e062de33e103343281ececdd645f8632", "Test")
    val wrapped = BitMessageTransport.wrap(message)
    wrapped shouldBe """[{"hash":"95a8c5c2ddec08a2eac00a24281bf5a2","message":"VGVzdA==","replyTo":"e062de33e103343281ececdd645f8632"}]"""
    BitMessageTransport.unwrap(wrapped) shouldBe Vector(message)
  }

  it should "decode message" in {
    BitMessageTransport.unwrap("""[{"hash":"90b90b6b88eeaf0660f260bd6bedcdcf","message":"W2ddVHVlLCA4L01hci8yMDE2LCAwMjozNjo1My45OTkgKEV1cm9wZS9Nb3Njb3cpLCBjbGllbnQ6IGthcmFzaXEtbmFub2JvYXJkIHYxLjAuM1svZ10K0KLQtdGB0YIg0LLQvdC10YjQvdC10Lkg0LrQsNGA0YLQuNC90LrQuApbc2ltZz1odHRwOi8vZnM1LmRpcmVjdHVwbG9hZC5uZXQvaW1hZ2VzLzE2MDMwOC95aWhwOTNzbC5wbmdd","replyTo":"f1fbb838193bb358c9d00fdfcfa028fe"}]""") shouldBe Vector(NanoboardMessage("f1fbb838193bb358c9d00fdfcfa028fe", "[g]Tue, 8/Mar/2016, 02:36:53.999 (Europe/Moscow), client: karasiq-nanoboard v1.0.3[/g]\nТест внешней картинки\n[simg=http://fs5.directupload.net/images/160308/yihp93sl.png]"))
  }
}
