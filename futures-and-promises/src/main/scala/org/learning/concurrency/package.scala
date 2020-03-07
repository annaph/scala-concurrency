package org.learning

package object concurrency {

  def log(msg: String): Unit =
    println(s"${Thread.currentThread.getName}: $msg")

}
