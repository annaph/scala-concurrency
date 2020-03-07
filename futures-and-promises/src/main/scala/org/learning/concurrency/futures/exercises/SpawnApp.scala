package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise, blocking}
import scala.sys.process._
import scala.util.{Failure, Success, Try}

object SpawnApp extends App {

  val cmd = "ping -c 1 google.com"

  val futures = for (_ <- 0 until 7) yield spawn(cmd)

  val completedFutures = for (f <- futures) yield {
    Await.ready(f, Duration.Inf)
  }

  completedFutures.foreach { f =>
    f.value.get match {
      case Success(value) =>
        log(s"result = $value")
      case Failure(e) =>
        log(s"Error !!! ${e.toString}")
    }
  }

  def spawn(command: String): Future[Int] = {
    val p = Promise[Int]

    Future {
      blocking {
        p complete Try(command !)
      }
    }

    p.future
  }

}
