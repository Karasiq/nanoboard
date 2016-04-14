package com.karasiq.nanoboard.server.utils

import java.util.concurrent.{Executors, TimeUnit, TimeoutException}
import javax.script.ScriptEngine

import akka.util.ByteString
import jdk.nashorn.api.scripting.NashornScriptEngineFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps

object FractalMusic extends FractalMusicGenerator

class FractalMusicGenerator {
  private val scheduler = Executors.newScheduledThreadPool(1)
  private val engineFactory = new NashornScriptEngineFactory()

  protected def createScriptEngine(): ScriptEngine = {
    engineFactory.getScriptEngine(Array("-strict", "--no-java", "--no-syntax-extensions"), getClass.getClassLoader)
  }

  def apply(formula: String, timeLimit: FiniteDuration = 5 seconds): Future[ByteString] = {
    val source =
      s"""
         |(function(){
         |  var sampleRate = 8000;
         |  var data = []
         |  for (var t = 0; t < 4*65536; t++) {
         |    data[t] = $formula;
         |    data[t] = (data[t] & 0xff) / 256.0;
         |  }
         |
         |  var n = data.length;
         |  var integer = 0, i;
         |  var header = 'RIFF<##>WAVEfmt \\x10\\x00\\x00\\x00\\x01\\x00\\x01\\x00<##><##>\\x01\\x00\\x08\\x00data<##>';
         |
         |  function insertLong(value) {
         |    var bytes = "";
         |    for (i = 0; i < 4; ++i) {
         |      bytes += String.fromCharCode(value % 256);
         |      value = Math.floor(value / 256);
         |    }
         |    header = header.replace('<##>', bytes);
         |  }
         |
         |  insertLong(36 + n);
         |  insertLong(sampleRate);
         |  insertLong(sampleRate);
         |  insertLong(n);
         |
         |  for (var i = 0; i < n; ++i) {
         |    header += String.fromCharCode(Math.round(Math.min(1, Math.max(-1, data[i])) * 127 + 127));
         |  }
         |  return header;
         |})();
    """.stripMargin

    val promise = Promise[ByteString]

    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          val engine = createScriptEngine()
          promise.success(ByteString(engine.eval(source).asInstanceOf[String].toCharArray.map(_.toByte)))
        } catch {
          case exc: Throwable ⇒
            promise.failure(exc)
        }
      }
    })

    scheduler.schedule(new Runnable {
      override def run(): Unit = {
        if (!promise.isCompleted) {
          thread.interrupt()
          promise.failure(new TimeoutException("JavaScript execution timed out"))
        }
      }
    }, timeLimit.toMillis, TimeUnit.MILLISECONDS)

    thread.start()
    promise.future
  }
}
