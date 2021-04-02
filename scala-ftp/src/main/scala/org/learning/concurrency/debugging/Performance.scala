package org.learning.concurrency.debugging

import org.learning.concurrency.debugging.Correctness.Accumulator
import org.scalameter._

import java.util.concurrent.atomic.{AtomicLong, AtomicLongArray}
import scala.annotation.tailrec
import scala.math.abs
import scala.util.hashing.byteswap32

object Performance extends App {

  println(s"===> Number of available processors: ${Runtime.getRuntime.availableProcessors}")

  val time = measure {
    action()
  }

  println(f"===> Running time: ${time.value}%1.2f ${time.units}")

  val configParameters = Seq(
    KeyValue(Key.exec.minWarmupRuns -> 12),
    KeyValue(Key.exec.maxWarmupRuns -> 51),
    KeyValue(Key.exec.benchRuns -> 31),
    KeyValue(Key.verbose -> true)
  )

  val accTime = config(configParameters: _*)
    .withWarmer(new Warmer.Default)
    .measure {
      action()
    }

  println(f"===> Accumulator time: ${accTime.value}%1.2f ${time.units}")

  val longAccTime = config(configParameters: _*)
    .withWarmer(new Warmer.Default)
    .measure {
      val longAccumulator = new LongAccumulator(0L)(_ + _)
      for (i <- 1 to 1000000) longAccumulator add i
    }

  println(f"===> Long accumulator time: ${longAccTime.value}%1.2f ${longAccTime.units}")

  val longAccTime4 = config(configParameters: _*)
    .withWarmer(new Warmer.Default)
    .measure {
      val longAccumulator = new LongAccumulator(0L)(_ + _)

      val total = 1000000
      val numOfThreads = 4

      val threads = for (i <- 0 until numOfThreads) yield thread {
        val start = ((i * total) / numOfThreads) + 1
        val end = total / numOfThreads

        for (j <- start to end) longAccumulator add j
      }

      threads.foreach(_.join())
    }

  println(f"===>  4 threads long accumulator time: ${longAccTime4.value}%1.2f ${longAccTime4.units}")

  val parLongAccTime = config(configParameters: _*)
    .withWarmer(new Warmer.Default)
    .measure {
      val parLongAccumulator = new ParLongAccumulator(0L)(_ + _)

      val total = 1000000
      val numOfThreads = 4

      val threads = for (i <- 0 until numOfThreads) yield thread {
        val start = ((i * total) / numOfThreads) + 1
        val end = total / numOfThreads

        for (j <- start to end) parLongAccumulator add j
      }

      threads.foreach(_.join())
    }

  println(f"===> Parallel long accumulator time: ${parLongAccTime.value}%1.2f ${parLongAccTime.units}")

  def action(): Unit = {
    val accumulator = new Accumulator(0L)(_ + _)
    for (i <- 1 to 1000000) accumulator add i
  }

  class LongAccumulator(z: Long)(op: (Long, Long) => Long) {

    private val value = new AtomicLong(z)

    def apply(): Long = value.get()

    @tailrec
    final def add(t: Long): Unit = {
      val oldValue = value.get()
      val newValue = op(oldValue, t)

      if (!value.compareAndSet(oldValue, newValue)) add(t)
    }

  }

  class ParLongAccumulator(z: Long)(op: (Long, Long) => Long) {

    private val par = Runtime.getRuntime.availableProcessors * 128

    private val values = new AtomicLongArray(par)

    def apply(): Long = {
      @tailrec
      def go(i: Int = 0, acc: Long): Long =
        if (i == par) acc else go(i + 1, op(acc, values.get(i)))

      go(acc = z)
    }

    @tailrec
    final def add(t: Long): Unit = {
      val threadId = Thread.currentThread.getId.toInt
      val i = abs(byteswap32(threadId)) % par

      val oldValue = values.get(i)
      val newValue = op(oldValue, t)

      if (!values.compareAndSet(i, oldValue, newValue)) add(t)
    }

  }

}
