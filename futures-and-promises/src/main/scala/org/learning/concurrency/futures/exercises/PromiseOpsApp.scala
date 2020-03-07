package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object PromiseOpsApp extends App {

  val pT = Promise[String]
  val pS: Promise[Int] = pT.compose(x => s"val = $x")

  pS success 3
  pT.future.foreach(log)

  val pTFailure = Promise[String]
  val pSFailure: Promise[Int] = pTFailure.compose(x => s"val = $x")

  pSFailure failure new Exception
  pTFailure.future.failed.foreach { e =>
    log(s"s failed with $e")
  }

  Thread sleep 3000

  implicit class PromiseOps[T](self: Promise[T]) {

    def compose[S](f: S => T): Promise[S] = {
      val p = Promise[S]

      p.future.onComplete {
        case Success(s) =>
          Future(self trySuccess f(s))
        case Failure(e) =>
          self tryFailure e
      }

      p
    }
  }

}
