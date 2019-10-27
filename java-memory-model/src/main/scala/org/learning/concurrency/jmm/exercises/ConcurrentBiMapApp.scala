package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.{log, thread}

import scala.collection.mutable

object ConcurrentBiMapApp extends App {

  val biMap = new ConcurrentBiMap[Int, String]
  val inverseBiMap = new ConcurrentBiMap[String, Int]

  for {
    _ <- 1 to 3
  } thread {
    for (i <- 1 to 1000000) biMap put(i, s"<-$i")
  }

  Thread sleep 30000

  biMap.iterator.foreach(str => log(s"$str "))
  log(s"BiMap size is ${biMap.size}")

  for {
    _ <- 1 to 3
  } thread {
    for (i <- 1 to 1000000) {
      val value = biMap getValue i
      inverseBiMap put(value.get, i)
    }
  }

  Thread sleep 30000

  inverseBiMap.iterator.foreach(str => log(s"$str "))
  log(s"Inverse BiMap size is ${inverseBiMap.size}")

}

object ConcurrentBiMapApp2 extends App {

  val biMap = new ConcurrentBiMap[Int, Int]

  val t1 = thread {
    for (i <- 1 to 101) biMap put(i, -i)
  }

  t1.join()

  biMap.iterator.foreach(i => log(s"$i "))
  log(s"BiMap size is ${biMap.size}")

  for {
    _ <- 1 to 3
  } thread {
    for (i <- 1 to 101) biMap replace(i, -i, -i, i)
  }

  Thread sleep 3000

  biMap.iterator.foreach(i => log(s"$i "))
  log(s"BiMap size is ${biMap.size}")

}

class ConcurrentBiMap[K, V] {

  private val map = mutable.Map.empty[K, V]

  private val inverse = mutable.Map.empty[V, K]

  def getValue(key: K): Option[V] = this.synchronized {
    map get key
  }

  def getKey(value: V): Option[K] = this.synchronized {
    inverse get value
  }

  def put(key: K, value: V): Option[V] = this.synchronized {
    ConcurrentBiMap put(key, value, map, inverse)
  }

  def removeKey(key: K): Option[V] = this.synchronized {
    ConcurrentBiMap remove(key, map, inverse)
  }

  def removeValue(value: V): Option[K] = this.synchronized {
    ConcurrentBiMap remove(value, inverse, map)
  }

  def exists(key: K, value: V): Boolean = this.synchronized {
    ConcurrentBiMap exists(key, value, this)
  }

  def replace(k1: K, v1: V, k2: K, v2: V): Unit = this.synchronized {
    ConcurrentBiMap replace(k1, v1, k2, v2, this)
  }

  def size: Int = this.synchronized {
    map.size
  }

  def iterator: Iterator[(K, V)] =
    map.iterator

}

object ConcurrentBiMap {

  private def put[K, V](key: K, value: V, map: mutable.Map[K, V], inverse: mutable.Map[V, K]): Option[V] = {
    def putInMap(): Unit = map put(key, value)

    def putInInverseMap(): Unit = inverse put(value, key)

    def putAndRemoveFromMap(k: K): Unit = {
      map put(key, value)
      map remove k
    }

    def putAndRemoveFromInverseMap(v: V): Unit = {
      inverse put(value, key)
      inverse remove v
    }

    (map get key, inverse get value) match {
      case (x@Some(v), Some(k)) if k == key && v == value =>
        x
      case (x@Some(v), Some(k)) =>
        putAndRemoveFromMap(k)
        putAndRemoveFromInverseMap(v)
        x
      case (x@Some(v), None) =>
        putAndRemoveFromInverseMap(v)
        putInMap()
        x
      case (None, Some(k)) =>
        putAndRemoveFromMap(k)
        putInInverseMap()
        Some(value)
      case (None, None) =>
        putInMap()
        putInInverseMap()
        None
    }
  }

  private def exists[K, V](key: K, value: V, m: ConcurrentBiMap[K, V]): Boolean = {
    (m getValue key, m getKey value) match {
      case (Some(v), Some(k)) if k == key && v == value =>
        true
      case _ =>
        false
    }
  }

  private def replace[K, V](k1: K, v1: V, k2: K, v2: V, m: ConcurrentBiMap[K, V]): Unit =
    if (m.exists(k1, v1)) {
      m removeKey k1
      m put(k2, v2)
    }

  private def remove[A, B](x: A, m1: mutable.Map[A, B], m2: mutable.Map[B, A]): Option[B] =
    m1.remove(x) match {
      case x@Some(b) =>
        m2 remove b
        x
      case None =>
        None
    }

}
