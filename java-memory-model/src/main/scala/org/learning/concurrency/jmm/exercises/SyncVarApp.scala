package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.{log, thread}

object SyncVarApp extends App {

  val syncVar: SyncVar[Int] = new SyncVar
  syncVar put 1

  Thread sleep 3000

  val t = thread {
    log(s"syncVar value: ${syncVar.get()}")
  }

  t.join()

}

object SyncVarGetExceptionApp extends App {

  val syncVar: SyncVar[Int] = new SyncVar

  val t = thread {
    log(s"syncVar value: ${syncVar.get()}}")
  }

  t.join()

}

object SyncVarPutExceptionApp extends App {

  val syncVar: SyncVar[Int] = new SyncVar
  syncVar put 1

  Thread sleep 3000

  val t = thread {
    syncVar put 2
  }

  t.join()

}

class SyncVar[T] {

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

}
