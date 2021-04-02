package org.learning.concurrency.debugging

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

object Correctness extends App {

  for (_ <- 1 to 100) {
    val fs = for (i <- 1 to 3) yield Future(i)
    val folded = fold(fs)(0)(_ + _)

    val result = Await.result(folded, 1.second)
    require(result == 6, s"(1 + 2 + 3) should be 6, not $result!")
  }

  def fold[T](fs: Seq[Future[T]])(z: T)(op: (T, T) => T): Future[T] = {
    val p = Promise[T]()

    val accumulator = new Accumulator(z)(op)

    val countDownLatch = new CountDownLatch(fs.length)(onComplete = {
      val result = accumulator()
      p success result
    })

    for (f <- fs) f.foreach { n =>
      accumulator add n
      countDownLatch.count()
    }

    p.future
  }

  class Accumulator[T](z: T)(op: (T, T) => T) {

    private val value = new AtomicReference[T](z)

    def apply(): T = value.get()

    @tailrec
    final def add(t: T): Unit = {
      val oldValue = value.get()
      val newValue = op(oldValue, t)

      if (!value.compareAndSet(oldValue, newValue)) add(t)
    }

  }

  class CountDownLatch(n: Int)(onComplete: => Unit) {

    private val counter = new AtomicInteger(n)

    def count(): Unit =
      if (counter.decrementAndGet() == 0) onComplete

  }

}
