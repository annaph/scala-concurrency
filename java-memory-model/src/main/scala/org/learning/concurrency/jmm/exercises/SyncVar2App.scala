package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.{log, thread}

object SyncVar2App extends App {

  val syncVar: SyncVar2[Int] = new SyncVar2

  val producer = thread {
    LazyList.continually(syncVar.isEmpty)
      .filter(identity)
      .zipWithIndex
      .map(_._2)
      .take(15)
      .foreach(i => syncVar put (i + 1))
  }

  val consumer = thread {
    LazyList.continually(syncVar.nonEmpty)
      .filter(identity)
      .take(15)
      .foreach(_ => log(s"get = ${syncVar.get()}"))
  }

  producer.join()
  consumer.join()

}

class SyncVar2[T] {

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

}
