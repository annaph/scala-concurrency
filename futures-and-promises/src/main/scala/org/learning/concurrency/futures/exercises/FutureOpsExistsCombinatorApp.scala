package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FutureOpsExistsCombinatorApp extends App {

  val f: Int => Boolean = _ % 2 == 0

  Future(2).exists(f).foreach { _ =>
    log(s"'2' is even")
  }

  Future(3).exists(f).foreach { _ =>
    log(s"'3' is odd")
  }

  Future[Long](throw new Exception).exists(_ => true).foreach { x =>
    log(s"Error: '$x'")
  }

  Thread sleep 3000

  implicit class FutureOps[T](self: Future[T]) {

    def exists(f: T => Boolean): Future[Boolean] =
      self.map(f).recover(_ => false)

  }

}
