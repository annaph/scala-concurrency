package org.learning.concurrency

package object debugging {

  def thread(body: => Unit): Thread = {
    val t = new Thread {
      override def run(): Unit = body
    }

    t.start()
    t
  }

}
