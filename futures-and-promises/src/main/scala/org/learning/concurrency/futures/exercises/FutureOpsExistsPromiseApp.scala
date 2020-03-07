package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object FutureOpsExistsPromiseApp extends App {

  val f: Int => Boolean = _ % 2 == 0

  Future(2).exists(f).foreach { _ =>
    log(s"'2' is even")
  }

  Future(3).exists(f).foreach { _ =>
    log(s"'3' is odd")
  }

  Future[Int](throw new Exception).exists(f).foreach { x =>
    log(s"Error: '$x'")
  }

  Thread sleep 3000

  implicit class FutureOps[T](self: Future[T]) {

    def exists(f: T => Boolean): Future[Boolean] = {
      val p = Promise[Boolean]

      self.onComplete {
        case Success(value) =>
          p success f(value)
        case Failure(_) =>
          p success false
      }

      p.future
    }

  }

}
