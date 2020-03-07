package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

object IVarApp extends App {

  val v = new IVar[Int]
  v := 3

  log(s"v = ${v()}")

  Thread sleep 1000

  for (_ <- 0 until 7) {
    val x = new IVar[Int]

    val f1 = Future(x := 1)
    val f2 = Future(x := 3)
    val f3 = Future(x := 7)

    Await.ready(f1, Duration.Inf)
    Await.ready(f2, Duration.Inf)
    Await.ready(f3, Duration.Inf)

    log(s"f1: $f1")
    log(s"f2: $f2")
    log(s"f3: $f3")

    log(s"x = ${x()}")
    log("---------------------------------------")
  }

}

class IVar[T] {

  private val _p = Promise[T]

  def apply(): T =
    if (_p.isCompleted) Await.result(_p.future, Duration.Inf) else throw new NoSuchElementException

  def :=(x: T): Unit =
    if (!_p.trySuccess(x)) throw new UnsupportedOperationException

}
