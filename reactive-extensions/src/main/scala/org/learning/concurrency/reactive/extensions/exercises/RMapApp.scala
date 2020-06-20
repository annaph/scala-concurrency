package org.learning.concurrency.reactive.extensions.exercises

import org.learning.concurrency.reactive.extensions.RMap

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object RMapApp extends App {

  val rMap = new RMap[String, Int]

  val key = "a"
  val observable = rMap(key)
  assert(!(rMap hasSubscribers key))

  val buffer1 = mutable.ListBuffer.empty[Int]
  val disposable1 = observable.subscribe(buffer1 += _)

  val buffer2 = mutable.ListBuffer.empty[Int]
  val disposable2 = observable.subscribe(buffer2 += _)

  rMap.update(key, 1)
  rMap.update(key, 2)
  assert(buffer1 == ListBuffer(1, 2))
  assert(buffer2 == ListBuffer(1, 2))

  disposable1.dispose()
  assert(rMap hasSubscribers key)

  disposable2.dispose()
  assert(!(rMap hasSubscribers key))

}
