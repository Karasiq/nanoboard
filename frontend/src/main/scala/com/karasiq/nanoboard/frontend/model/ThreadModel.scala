package com.karasiq.nanoboard.frontend.model

import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.nanoboard.frontend.api.{NanoboardApi, NanoboardMessageData}
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import rx._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[frontend] object ThreadModel {
  def apply(context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) = {
    new ThreadModel(context, postsPerPage)
  }
}

private[frontend] final class ThreadModel(val context: Var[NanoboardContext], postsPerPage: Int)(implicit ec: ExecutionContext, ctx: Ctx.Owner, controller: NanoboardController) {
  import controller.locale
  val addedPosts = Var(Set.empty[String])
  val deletedPosts = Var(Set.empty[String])
  val categories = Var(Vector.empty[NanoboardMessageData])
  val posts = Var(Vector.empty[NanoboardMessageData])

  def addPost(post: NanoboardMessageData): Unit = {
    if (!addedPosts.now.contains(post.hash) && !posts.now.exists(_.hash == post.hash)) {
      categories() = categories.now.collect {
        case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
          msg.copy(answers = answers + 1)

        case msg ⇒
          msg
      }

      val updated = context.now match {
        case NanoboardContext.Recent(0) ⇒
          if (posts.now.length == postsPerPage) {
            post +: posts.now.dropRight(1)
          } else {
            post +: posts.now
          }

        case NanoboardContext.Pending(_) if posts.now.length < postsPerPage ⇒
          posts.now :+ post

        case NanoboardContext.Thread(hash, 0) if post.parent.contains(hash) ⇒
          val (opPost, answers) = posts.now.partition(_.hash == hash)
          if (answers.length >= postsPerPage) {
            opPost ++ Some(post) ++ answers.dropRight(1)
          } else {
            opPost ++ Some(post) ++ answers
          }

        case NanoboardContext.Thread(post.hash, _) ⇒
          val (_, answers) = posts.now.partition(_.hash == post.hash)
          post +: answers

        case _ ⇒
          posts.now
      }
      posts() = updated.collect {
        case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(hash) ⇒
          msg.copy(answers = answers + 1)

        case msg ⇒
          msg
      }
      addedPosts() = addedPosts.now + post.hash
    }
  }

  private def deleteBy(post: NanoboardMessageData, f: (NanoboardMessageData) ⇒ Boolean): Unit = {
    if (!deletedPosts.now.contains(post.hash)) {
      categories() = categories.now.filterNot(f)
      context.now match {
        case NanoboardContext.Thread(post.hash, _) ⇒
          context() = post.parent.fold[NanoboardContext](NanoboardContext.Categories)(NanoboardContext.Thread(_))

        case NanoboardContext.Thread(_, _) ⇒
          val (Vector(opPost), answers) = posts.now.splitAt(1)
          val filtered = answers.filterNot(f)
          posts() = opPost.copy(answers = opPost.answers - 1) +: filtered
          deletedPosts() = deletedPosts.now ++ answers.diff(filtered).map(_.hash)

        case _ ⇒
          val current = posts.now
          val filtered = current.filterNot(f)
          posts() = filtered
          deletedPosts() = deletedPosts.now ++ current.diff(filtered).map(_.hash)
      }

      posts() = posts.now.collect {
        case msg @ NanoboardMessageData(_, hash, _, answers) if post.parent.contains(post.hash) ⇒
          msg.copy(answers = answers - 1)

        case msg ⇒
          msg
      }
    }
  }

  def deleteTree(post: NanoboardMessageData): Unit = {
    deleteBy(post, p ⇒ p.hash == post.hash || p.parent.contains(post.hash))
  }

  def deleteSingle(post: NanoboardMessageData): Unit = {
    deleteBy(post, _.hash == post.hash)
  }

  def updatePosts(): Unit = {
    val future = context.now match {
      case NanoboardContext.Categories ⇒
        Future.successful(categories.now)

      case NanoboardContext.Thread(hash, offset) ⇒
        NanoboardApi.thread(hash, offset, postsPerPage)

      case NanoboardContext.Recent(offset) ⇒
        NanoboardApi.recent(offset, postsPerPage)

      case NanoboardContext.Pending(offset) ⇒
        NanoboardApi.pending(offset, postsPerPage)
    }

    future.onComplete {
      case Success(posts) ⇒
        this.addedPosts() = Set.empty
        this.deletedPosts() = Set.empty
        this.posts() = posts

      case Failure(exc) ⇒
        Notifications.error(exc)(locale.updateError, Layout.topRight)
    }
  }

  def updateCategories(): Unit = {
    NanoboardApi.categories().onComplete {
      case Success(categories) ⇒
        this.categories() = categories

      case Failure(exc) ⇒
        Notifications.error(exc)(locale.updateError, Layout.topRight)
    }
  }

  // Initialization
  context.foreach(_ ⇒ updatePosts())
  categories.foreach { categories ⇒
    if (context.now == NanoboardContext.Categories) {
      addedPosts() = Set.empty
      deletedPosts() = Set.empty
      posts() = categories
    }
  }
  updateCategories()
}
