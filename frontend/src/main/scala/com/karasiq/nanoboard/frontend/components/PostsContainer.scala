package com.karasiq.nanoboard.frontend.components

import com.karasiq.nanoboard.frontend.{NanoboardContext, NanoboardMessageData}
import rx.Rx

trait PostsContainer {
  def context: Rx[NanoboardContext]
  def posts: Rx[Vector[NanoboardMessageData]]
  def addPost(post: NanoboardMessageData): Unit
  def deletePost(post: NanoboardMessageData): Unit
  def update(): Unit
}
