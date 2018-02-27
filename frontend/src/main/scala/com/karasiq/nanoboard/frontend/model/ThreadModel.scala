package com.karasiq.nanoboard.frontend.model

import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

import rx._

import com.karasiq.bootstrap.Bootstrap.default._
import scalaTags.all._

import com.karasiq.nanoboard.api.NanoboardMessageData
import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardController}
import com.karasiq.nanoboard.frontend.api.NanoboardApi
import com.karasiq.nanoboard.frontend.utils.Notifications
import com.karasiq.nanoboard.frontend.utils.Notifications.Layout

private[frontend] object ThreadModel {
  def apply(context: Var[NanoboardContext], postsPerPage: Int)(implicit controller: NanoboardController) = {
    new ThreadModel(context, postsPerPage)
  }
}

private[frontend] final class ThreadModel(val context: Var[NanoboardContext], postsPerPage: Int)(implicit controller: NanoboardController) {
  import controller.locale

  val addedPosts = Var(Set.empty[String])
  val deletedPosts = Var(Set.empty[String])
  val categories = Var(Vector.empty[NanoboardMessageData])
  val posts = Var(Vector.empty[NanoboardMessageData])

  def addPost(post: NanoboardMessageData): Unit = {
    if (!addedPosts.now.contains(post.hash) && !posts.now.exists(_.hash == post.hash)) {
      posts() = context.now match {
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
      updateAnswersCount(+1, post, posts)
      addedPosts() = addedPosts.now + post.hash
      deletedPosts() = deletedPosts.now - post.hash
      updateAnswersCount(+1, post, categories)
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

  private[this] def updateAnswersCount(i: Int, post: NanoboardMessageData, posts: Var[Vector[NanoboardMessageData]]): Unit = {
    posts() = posts.now.collect {
      case msg @ NanoboardMessageData(_, _, hash, _, answers, _, _) if post.parent.contains(hash) ⇒
        msg.copy(answers = answers + i)

      case msg ⇒
        msg
    }
  }

  private[this] def deleteBy(post: NanoboardMessageData, f: (NanoboardMessageData) ⇒ Boolean): Unit = {
    if (!deletedPosts.now.contains(post.hash)) {
      context.now match {
        case NanoboardContext.Thread(post.hash, _) ⇒
          context() = post.parent.fold[NanoboardContext](NanoboardContext.Categories)(NanoboardContext.Thread(_))
          deletedPosts() = deletedPosts.now + post.hash

        case NanoboardContext.Thread(hash, _) ⇒
          val (opPost, answers) = posts.now.partition(_.hash == hash)
          val filtered = answers.filterNot(f)
          posts() = opPost.map(p ⇒ p.copy(answers = p.answers - 1)) ++ filtered
          deletedPosts() = deletedPosts.now ++ answers.diff(filtered).map(_.hash)

        case _ ⇒
          val current = posts.now
          val filtered = current.filterNot(f)
          posts() = filtered
          deletedPosts() = deletedPosts.now ++ current.diff(filtered).map(_.hash)
      }

      updateAnswersCount(-1, post, posts)
      categories() = categories.now.filterNot(f)
      updateAnswersCount(-1, post, categories)
      addedPosts() = addedPosts.now - post.hash
    }
  }

  // Initialization
  private[this] def initialize(): Unit = {
    context.foreach(_ ⇒ updatePosts())
    categories.foreach { categories ⇒
      if (context.now == NanoboardContext.Categories && posts.now != categories) {
        addedPosts() = Set.empty
        deletedPosts() = Set.empty
        posts() = categories
      }
    }
    updateCategories()
  }

  initialize()
}
