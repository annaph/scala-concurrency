package org.learning.concurrency.blocks.exercises

import org.learning.concurrency.blocks.{log, thread}

class LazyCell[T](initialization: => T) {

  @volatile private var _bitmap = false
  private var _value: T = _

  def apply(): T = if (_bitmap) _value else this.synchronized {
    if (_bitmap) _value else {
      _value = initialization
      _bitmap = true
      _value
    }
  }

}

object LazyCellApp extends App {

  val a = new LazyCell[Int](func)

  val threads = for (_ <- 1 to 12) yield {
    thread {
      Thread sleep (Math.random * 10).toInt
      log(s"a = ${a()}")
    }
  }

  def func: Int = {
    log("Calculating...")
    Thread sleep 12000
    3
  }

  threads.foreach(_.join())

}
