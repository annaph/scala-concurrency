package org.learning.concurrency

import scala.concurrent.ExecutionContext

package object blocks {

  def log(msg: String): Unit =
    println(s"${Thread.currentThread.getName}: $msg")

  def thread(body: => Unit): Thread = {
    val t = new Thread {
      override def run(): Unit = body
    }

    t.start()
    t
  }

  def execute(body: => Unit): Unit =
    ExecutionContext.global.execute(() => body)

}
