package com.karasiq.nanoboard.frontend.components

import com.karasiq.bootstrap.BootstrapComponent
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.styles.BoardStyle
import com.karasiq.nanoboard.frontend.{NanoboardApi, NanoboardMessageData}
import rx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

final class NanoboardThread(currentThread: Rx[Option[String]], style: BoardStyle)(implicit ec: ExecutionContext, ctx: Ctx.Owner) extends BootstrapComponent {
  private val posts_ : Var[Vector[NanoboardMessageData]] = Var(Vector.empty)

  def posts: Rx[Vector[NanoboardMessageData]] = this.posts_

  currentThread.foreach { hash ⇒
    val future = hash.fold(NanoboardApi.categories())(NanoboardApi.answers)
    future.onComplete {
      case Success(posts) ⇒
        this.posts_.update(posts)

      case Failure(exc) ⇒
        println(s"Nanoboard thread error: $exc")
        this.posts_.update(Vector.empty)
    }
  }

  override def render(md: Modifier*): Modifier = Rx[Frag] {
    val thread = posts()
    div(for {
      opPost ← thread.headOption.map(new NanoboardPost(true, style, _))
      answers ← Some(thread.tail.map(new NanoboardPost(false, style, _)))
    } yield opPost +: answers)
  }
}
