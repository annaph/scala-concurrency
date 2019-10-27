package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.{log, thread}

object SyncQueueApp extends App {

  val queue = new SyncQueue[Int](3)

  val producer = thread {
    for (x <- 1 to 15) queue put x
  }

  val consumer = thread {
    for (_ <- 1 to 15) log(s"get = ${queue.get()}")
  }

  producer.join()
  consumer.join()

}

class SyncQueue[T](n: Int) {

  import scala.collection.mutable

  private val queue = mutable.Queue.empty[T]

  def get(): T = this.synchronized {
    while (queue.isEmpty) this.wait()

    val v = queue.dequeue()
    this.notify()

    v
  }

  def put(x: T): Unit = this.synchronized {
    while (queue.size == n) this.wait()

    queue += x
    this.notify()
  }

}
