package org.learning.concurrency.futures

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, blocking}
import scala.io.Source

object BlockingAwait extends App {

  val urlSpecSizeFuture = Future {
    val f = Source fromURL "https://www.w3.org/Addressing/URL/url-spec.txt"
    try f.size finally f.close()
  }

  val urlSpecSize = Await.result(urlSpecSizeFuture, 31.seconds)

  log(s"url spec contains $urlSpecSize characters")

}

object BlockingSleepBad extends App {

  val startTime = System.nanoTime()

  val futures = for (_ <- 0 until 64) yield Future {
    Thread sleep 1000
  }

  for (f <- futures)
    Await.result(f, Duration.Inf)

  val endTime = System.nanoTime()

  log(s"Total execution time of the program = ${(endTime - startTime) / 1000000} ms")
  log(s"Note: there are ${Runtime.getRuntime.availableProcessors} CPUs on this machine")

}

object BlockingSleepOk extends App {

  val startTime = System.nanoTime()

  val futures = for (_ <- 0 until 64) yield Future {
    blocking {
      Thread sleep 1000
    }
  }

  for (f <- futures)
    Await.result(f, Duration.Inf)

  val endTime = System.nanoTime()

  log(s"Total execution time of the program = ${(endTime - startTime) / 1000000} ms")
  log(s"Note: there are ${Runtime.getRuntime.availableProcessors} CPUs on this machine")

}
