package org.learning.concurrency

package object jmm {

  def log(msg: String): Unit =
    println(s"${Thread.currentThread.getName}: $msg")

  def thread(body: => Unit): Thread = {
    val t = new Thread {
      override def run(): Unit = body
    }

    t.start()
    t
  }

}
