package org.learning.concurrency.blocks.exercises

import java.util.concurrent.atomic.AtomicReference

import org.learning.concurrency.blocks.{log, thread}

import scala.annotation.tailrec

class PureLazyCell[T](initialization: => T) {

  private val _value = new AtomicReference[Option[T]](None)

  @tailrec
  final def apply: T = {
    val oldValue = _value.get

    oldValue match {
      case Some(v) =>
        v
      case None =>
        val newValue = Some(initialization)
        if (_value.compareAndSet(oldValue, newValue)) newValue.get else apply
    }
  }

}

object PureLazyCellApp extends App {

  val a = new PureLazyCell[Int](func)

  val threads = for (_ <- 1 to 12) yield {
    thread {
      Thread sleep (Math.random * 10000).toInt
      log(s"a = ${a.apply}")
    }
  }

  def func: Int = {
    log("Calculating...")
    Thread sleep 1000
    3
  }

  threads.foreach(_.join())

}
