package org.learning.concurrency.transactional

package object memory {

  def log(msg: String): Unit = {
    println(s"${Thread.currentThread.getName}: $msg")
  }

}
