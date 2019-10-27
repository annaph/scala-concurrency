package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.log

object PeriodicallyComputationApp extends App {

  def periodically(duration: Long)(b: => Unit): Unit = {
    val worker = new Thread {
      override def run(): Unit =
        while (true) {
          b
          Thread sleep duration
        }
    }

    worker setName "Worker"
    worker setDaemon true

    worker.start()
  }

  periodically(1000)(log("Hello"))

  Thread sleep 12000
  log(s"Finnish executing ${Thread.currentThread().getName()} thread")

}
