package org.learning.concurrency.blocks

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}

import scala.annotation.tailrec
import scala.collection.{concurrent, mutable}
import scala.jdk.CollectionConverters._

object CollectionsBad extends App {

  val buffer = mutable.ArrayBuffer.empty[Int]

  def add(numbers: Seq[Int]): Unit = execute {
    buffer ++= numbers
    log(s"buffer=$buffer")
  }

  add(0 until 10)
  add(10 until 20)

  Thread sleep 3000

}

object CollectionsIterator extends App {

  val queue = new LinkedBlockingQueue[String]()

  for (i <- 1 to 5500) queue offer i.toString

  execute {
    val iterator = queue.iterator
    while (iterator.hasNext) log(iterator.next())
  }

  for (_ <- 1 to 5500) queue.poll()

  Thread sleep 3000

}

object CollectionsConcurrentMapBulk extends App {

  val names = new ConcurrentHashMap[String, Int]().asScala

  names put("Stacey", 0)
  names put("Chase", 0)
  names put("Loren", 0)

  execute {
    for (n <- 0 until 12) names put(s"Anna $n", n)
  }

  execute {
    log("no snapshot!")
    for (name <- names) log(s"name: $name")
  }

  Thread sleep 3000

}

object CollectionsTrieMapBulk extends App {

  val names = concurrent.TrieMap.empty[String, Int]

  names put("Stacey", 0)
  names put("Chase", 0)
  names put("Loren", 0)

  execute {
    for (n <- 1 to 7) names put(s"Anna $n", n)
  }

  execute {
    log("snapshot!")
    for (name <- names.map[String](_._1).toSeq.sorted) log(s"name: $name")
  }

  Thread sleep 3000

}

class AtomicBuffer[T] {

  private val buffer = new AtomicReference[List[T]](Nil)

  @tailrec
  final def +=(x: T): Unit = {
    val xs = buffer.get
    val xss = x :: xs

    if (!buffer.compareAndSet(xs, xss)) this += x
  }

}
