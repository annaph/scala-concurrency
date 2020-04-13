package org.learing.concurrency.parallel.collections.exercises

import org.learing.concurrency.parallel.collections.log

import scala.annotation.tailrec

object ObjectAllocationApp extends App {

  log(s"avg = $avg nanoseconds")

  def avg: Double = {
    @tailrec
    def go(numbers: LazyList[Int], n: Int, prevTime: Double, sumTime: Double): Double = n match {
      case 30 =>
        sumTime / n
      case _ =>
        val time = Timed.buildObjects(10000000)

        if (isIn10Percent(prevTime, time))
          go(numbers.tail, n + 1, time, sumTime + time)
        else
          go(numbers.tail, 0, time, 0.0)
    }

    go(LazyList from 0, 0, 0.0, 0.0)
  }

  def isIn10Percent(prevTime: Double, time: Double): Boolean = {
    val diff = (Math.abs(prevTime - time) / time) * 100
    if (diff < 10.0) true else false
  }

}

object Timed {

  @volatile var dummy: AnyRef = _

  def buildObjects(n: Int): Double = {
    val start = System.nanoTime()
    for (_ <- 0 until n)
      dummy = new Object
    val end = System.nanoTime()

    (end - start) / n.toDouble
  }

}
