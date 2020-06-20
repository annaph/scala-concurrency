package org.learning.concurrency.reactive.extensions.exercises

import org.learning.concurrency.reactive.extensions.RPriorityQueue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object RPriorityQueueApp extends App {

  val queue = new RPriorityQueue[Int]

  queue add 3
  queue add 1
  queue add 2

  val l = mutable.ListBuffer.empty[Int]
  val disposable = queue.popped.subscribe(l += _)
  assert(queue.hasSubscribers)

  assert(queue.pop() == 3)
  assert(queue.pop() == 2)
  assert(queue.pop() == 1)
  assert(l == ListBuffer(3, 2, 1))

  disposable.dispose()
  assert(!queue.hasSubscribers)

}
