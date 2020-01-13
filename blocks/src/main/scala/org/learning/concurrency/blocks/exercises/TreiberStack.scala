package org.learning.concurrency.blocks.exercises

import java.util.concurrent.atomic.AtomicReference

import org.learning.concurrency.blocks.{log, thread}

import scala.annotation.tailrec

object TreiberStackApp extends App {

  val stack = new TreiberStack[Int]

  val t1 = thread {
    for (i <- 1 to 7) {
      stack push i
      Thread sleep 1
    }
  }

  val t2 = thread {
    for (i <- 1 to 7) {
      stack push (i * 10)
      Thread sleep 1
    }
  }

  t1.join()
  t2.join()

  for (i <- 1 to 14)
    log(s"s[$i] = ${stack.pop()}")

}

class TreiberStack[T] {

  private val stack = new AtomicReference[List[T]](Nil)

  @tailrec
  final def push(t: T): Unit = {
    val oldList = stack.get
    val newList = t :: oldList

    if (!stack.compareAndSet(oldList, newList)) push(t)
  }

  @tailrec
  final def pop(): T = {
    val oldList = stack.get
    val newList = oldList.tail

    if (stack.compareAndSet(oldList, newList)) oldList.head else pop()
  }

}
