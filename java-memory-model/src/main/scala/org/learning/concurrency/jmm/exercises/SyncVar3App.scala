package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.{log, thread}

object SyncVar3App extends App {

  val syncVar: SyncVar3[Int] = new SyncVar3

  val producer = thread {
    for (x <- 1 to 15) syncVar putWait x
  }

  val consumer = thread {
    for (_ <- 1 to 15) log(s"get = ${syncVar.getWait()}")
  }

  producer.join()
  consumer.join()

}

class SyncVar3[T] {

  private var value: Option[T] = None

  def get(): T = this.synchronized {
    value match {
      case Some(v) =>
        value = None
        v
      case None =>
        throw new Exception("SyncVar must be non-empty")
    }
  }

  def put(x: T): Unit = this.synchronized {
    value match {
      case Some(v) =>
        throw new Exception("SyncVar must be empty")
      case None =>
        value = Some(x)
    }
  }

  def isEmpty: Boolean = this.synchronized {
    value.isEmpty
  }

  def nonEmpty: Boolean = this.synchronized {
    value.nonEmpty
  }

  def getWait(): T = this.synchronized {
    while (value.isEmpty) this.wait()

    val v = value.get
    value = None
    this.notify()

    v
  }

  def putWait(x: T): Unit = this.synchronized {
    while (value.nonEmpty) this.wait()

    value = Some(x)
    this.notify()
  }

}
