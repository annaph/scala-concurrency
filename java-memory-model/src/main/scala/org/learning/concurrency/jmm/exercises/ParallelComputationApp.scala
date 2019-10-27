package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.thread

object ParallelComputationApp extends App {

  def parallel[A, B](a: => A, b: => B): (A, B) = {
    var res1 = null.asInstanceOf[A]
    var res2 = null.asInstanceOf[B]

    val t1 = thread {
      res1 = a
    }

    val t2 = thread {
      res2 = b
    }

    t1.join()
    t2.join()

    res1 -> res2
  }

  val comp1 = "Hello"
  val comp2 = 1

  val result = parallel(comp1, comp2)
  println(s"result: $result")

}
