package org.learning.concurrency.blocks

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec

object PoolMain extends App {

  val check = new ConcurrentHashMap[Int, Unit]()
  val pool = new Pool[Int]

  val p = 7
  val num = 1000000

  val producers = for (i <- 0 until p) yield {
    thread {
      for (j <- 0 until num) pool add (i * num + j)
    }
  }

  producers.foreach(_.join())

  val consumers = for (_ <- 0 until p) yield {
    thread {
      for (_ <- 0 until num) pool.remove() match {
        case Some(v) =>
          check put(v, ())
        case None =>
          sys.error("Should be non-empty!")
      }
    }
  }

  consumers.foreach(_.join())

  for (k <- 0 until p * num)
    assert(check containsKey k)

  for (i <- 1 to 7)
    pool add i

  pool foreach println

}

class Pool[T] {

  type Bucket = (List[T], Long)

  val parallelism: Int = Runtime.getRuntime.availableProcessors * 32

  private val buckets = new Array[AtomicReference[Bucket]](parallelism)

  for (i <- buckets.indices)
    buckets(i) = new AtomicReference[Bucket](Nil -> 0L)

  def add(t: T): Unit = {
    val i = (Thread.currentThread.getId * t.## % buckets.length).toInt

    @tailrec
    def retry(): Unit = {
      val reference = buckets(i)
      val oldBucket = reference.get

      val (oldList, oldTimestamp) = oldBucket
      val newBucket = (t :: oldList) -> (oldTimestamp + 1)

      if (!reference.compareAndSet(oldBucket, newBucket)) retry()
    }

    retry()
  }

  def remove(): Option[T] = {
    @tailrec
    def retry(n: Int, sum: Long): (Long, Option[T]) = {
      val reference = buckets(n)
      val oldBucket = reference.get
      val (oldList, oldTimestamp) = oldBucket

      oldList match {
        case x :: xs =>
          val newBucket = xs -> (oldTimestamp + 1)
          if (reference.compareAndSet(oldBucket, newBucket)) sum -> Some(x) else retry(n, sum)
        case Nil =>
          (sum + oldTimestamp) -> None
      }
    }

    @tailrec
    def scan(i: Int, start: Int, sum: Long, witness: Long): Option[T] = i match {
      case curr if curr == buckets.length && sum == witness =>
        None
      case curr if curr == buckets.length && sum != witness =>
        scan(0, start, 0L, sum)
      case _ =>
        retry((start + i) % buckets.length, sum) match {
          case (newSum, None) =>
            scan(i + 1, start, newSum, witness)
          case (_, value) =>
            value
        }
    }

    val start = (Thread.currentThread.getId % buckets.length).toInt
    scan(0, start, 0L, -1L)
  }

  def foreach(f: T => Unit): Unit = {
    val iterator: Iterator[T] = new Iterator[T] {
      private var _currPos = 0
      private var _currBucket: Bucket = buckets(0).get
      private var _currElement: Option[T] = None

      move()

      override def hasNext: Boolean = _currElement match {
        case Some(_) =>
          true
        case None =>
          false
      }

      override def next(): T = _currElement match {
        case Some(value) =>
          move()
          value
        case None =>
          throw new NoSuchElementException("next on empty iterator")
      }

      @tailrec
      private def move(): Unit = (_currPos, _currBucket._1) match {
        case (x, Nil) if x == (buckets.length - 1) =>
          _currElement = None
        case (_, Nil) =>
          _currPos = _currPos + 1
          _currBucket = buckets(_currPos).get
          move()
        case (_, x :: xs) =>
          _currBucket = xs -> _currBucket._2
          _currElement = Some(x)
      }

    }

    iterator foreach f
  }

}
