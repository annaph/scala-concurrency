package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.{log, thread}

object MemoizedMethodApp extends App {

  val func: Int => String = i => {
    log(s"############ Function invoked for input '$i' ############")
    Thread sleep 3000
    i.toString
  }

  val memoizedFunc = MemoizedFunction cache func

  val t1 = thread {
    for (i <- 1 to 12) log(s"input: '$i'; result: '${memoizedFunc(i)}'")
  }

  val t2 = thread {
    for (i <- 1 to 7) log(s"input: '$i'; result: '${memoizedFunc(i)}'")
  }

  val t3 = thread {
    for (i <- 1 to 3) log(s"input: '$i'; result: '${memoizedFunc(i)}'")
  }

  t1.join()
  t2.join()
  t3.join()

  log("The thread below should print cached results")

  val t4 = thread {
    for (i <- 1 to 12) log(s"input: '$i'; result: '${memoizedFunc(i)}'")
  }

  t4.join()

}

object MemoizedMethodAndForkJoinPoolApp extends App {

  import java.util.concurrent.ForkJoinPool

  val func = (i: Int) => {
    log(s"############ Function invoked for input '$i' ############")
    Thread sleep 3000
    i.toString
  }

  val memoizedFunc = MemoizedFunction cache func

  val pool = new ForkJoinPool(4)

  for (i <- 1 to 12) {
    pool execute (() => log(s"input: '$i'; result: '${memoizedFunc(i)}'"))
  }

  for (i <- 1 to 7) {
    pool execute (() => log(s"input: '$i'; result: '${memoizedFunc(i)}'"))
  }

  for (i <- 1 to 3) {
    pool execute (() => log(s"input: '$i'; result: '${memoizedFunc(i)}'"))
  }

  Thread sleep 39000

  log("The thread below should print cached results")

  for (i <- 1 to 12) {
    pool execute (() => log(s"input: '$i'; result: '${memoizedFunc(i)}'"))
  }

  pool.shutdown()

  Thread sleep 3000

}

sealed trait Value[+V]

case class Calculated[V](value: V) extends Value[V]

case object BeingCalculated extends Value[Nothing]

class MemoizedFunction[K, V](f: K => V) extends (K => V) {

  import scala.collection.mutable

  private val cache = mutable.Map.empty[K, Value[V]]

  override def apply(k: K): V = {
    val value = preFuncApply(k)
    val v = funcApply(k, value)
    postFuncApply(k, v)

    v
  }

  private def preFuncApply(key: K): Value[V] = cache.synchronized {
    cache.get(key) match {
      case Some(value@Calculated(_)) =>
        value
      case Some(BeingCalculated) =>
        cache.wait()
        preFuncApply(key)
      case None =>
        cache put(key, BeingCalculated)
        BeingCalculated
    }
  }

  private def funcApply(key: K, value: Value[V]): V = value match {
    case Calculated(value) =>
      value
    case BeingCalculated =>
      f(key)
  }

  private def postFuncApply(key: K, value: V): Unit = cache.synchronized {
    cache.get(key) match {
      case Some(BeingCalculated) =>
        cache put(key, Calculated(value))
        cache.notify()
      case _ =>
        ()
    }
  }

}

object MemoizedFunction {

  def cache[K, V](f: K => V): K => V =
    new MemoizedFunction(f)

}
