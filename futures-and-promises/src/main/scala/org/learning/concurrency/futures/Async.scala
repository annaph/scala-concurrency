package org.learning.concurrency.futures

import org.learning.concurrency.log

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

object AsyncBasic extends App {

  val workerName: Future[String] = async {
    Thread.currentThread.getName
  }

  workerName.foreach { name =>
    log(s"Future completed by worker $name")
  }

  Thread sleep 3000

}

object AsyncWhile extends App {

  def simpleCount(): Future[Unit] = async {
    log("T-minus 3 seconds")
    await {
      delay(1)
    }

    log("T-minus 2 seconds")
    await(delay(1))

    log("T-minus 1 second")
    await(delay(1))

    log("done!")
  }

  def countdown(nSeconds: Int)(f: Int => Unit): Future[Unit] = async {
    var i = nSeconds
    while (i > 0) {
      f(i)
      await(delay(1))

      i -= 1
    }
  }

  def delay(nSeconds: Int): Future[Unit] = async {
    blocking {
      Thread sleep (nSeconds * 1000)
    }
  }

  simpleCount()

  Thread sleep 7000

  countdown(12)(n => log(s"T-minus $n seconds")).foreach { _ =>
    log("This program is over!")
  }

  Thread sleep 17000

}
