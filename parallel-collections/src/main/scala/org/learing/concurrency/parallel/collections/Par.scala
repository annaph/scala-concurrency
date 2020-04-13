package org.learing.concurrency.parallel.collections

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import org.learing.concurrency.parallel.collections.GenSeq.toGenSeq
import org.learing.concurrency.parallel.collections.GenSet.toGenSet

import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.{ForkJoinTaskSupport, ParSeq}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

object ParBasic extends App {

  val numbers = Random shuffle Vector.tabulate(5000000)(identity)

  val seqTime = timed {
    val n = numbers.max
    log(s"largest number: $n")
  }

  log(s"Sequential time: $seqTime ms")

  val parTime = timed {
    val n = numbers.par.max
    log(s"largest number: $n")
  }

  log(s"Parallel time: $parTime ms")

}

object ParUid extends App {

  val uid = new AtomicLong(0)

  val seqTime = timed {
    for (_ <- 0 to 10000000) uid.getAndIncrement()
  }

  log(s"Sequential time: $seqTime ms")

  val parTime = timed {
    for (_ <- (0 to 10000000).par) uid.getAndIncrement()
  }

  log(s"Parallel time: $parTime ms")

}

object ParNotGeneric extends App {

  val doc = Seq.tabulate(1000)(i => s"lorem ipsum " * (i % 10))

  def findLongest(xs: Seq[String]): Unit = {
    val line = xs.maxBy(_.length)
    log(s"Longest line - $line")
  }

  def findLongest(xs: ParSeq[String]): Unit = {
    val line = xs.maxBy(_.length)
    log(s"Longest line - $line")
  }

  findLongest(doc)
  findLongest(doc.par)

}

object ParConfig extends App {

  val taskPool = new ForkJoinPool(2)
  val myTaskSupport = new ForkJoinTaskSupport(taskPool)

  val numbers = Random.shuffle(Vector.tabulate(5000000)(identity))

  val parTime = timed {
    val parNumbers = numbers.par
    parNumbers.tasksupport = myTaskSupport

    val n = parNumbers.max
    log(s"largest number $n")
  }

  log(s"Parallel time $parTime ms")

}

object ParHtmlSpecSearch extends App {

  htmlSpec().onComplete {
    case Success(lines) =>
      log("Download complete!")

      val seqTime = search(lines)
      log(s"Sequential time $seqTime ms")

      val parTime = search(lines.par)
      log(s"Parallel time $parTime ms")
    case Failure(e) =>
      throw new Exception("Error download HTML Spec!", e)
  }

  def search(xs: GenSeq[String]): Double = warmedTimed() {
    xs.indexWhere(_ matches ".*TEXTAREA.*")
  }

  Thread sleep 7000

}

object ParNonParallelizableCollections extends App {

  val list = List.fill(1000000)("")
  val vector = Vector.fill(1000000)("")

  log(s"list conversion time:  ${timed(list.par)} ms")
  log(s"vector conversion time: ${timed(vector.par)} ms")

}

object ParNonParallelizableOperations extends App {

  htmlSpec().foreach { lines =>
    val seqTime = allMatches(lines)
    log(s"Sequential time - $seqTime ms")

    val parTime = allMatches(lines.par)
    log(s"Parallel time - $parTime ms")

    val aggregateTime = warmedTimed() {
      val seqOp: (String, String) => String = {
        case (acc, line) =>
          if (line matches ".*TEXTAREA.*") s"$acc\n$line" else acc
      }

      val combOp: (String, String) => String = {
        case (acc1, acc2) => acc1 + acc2
      }

      lines.par.aggregate("")(seqOp, combOp)
    }

    log(s"Aggregate time - $aggregateTime ms")
  }

  def allMatches(xs: GenSeq[String]): Double = warmedTimed() {
    xs.foldLeft("") { (acc, line) =>
      if (line matches ".*TEXTAREA.*") s"$acc\n$line" else acc
    }
  }

  Thread sleep 7000

}

object ParSideEffectsIncorrect extends App {

  val a = (0 until 1000).toSet
  val b = (0 until 1000 by 4).toSet

  val seqRes = intersectionSize(a, b)
  val parRes = intersectionSize(a.par, b.par)

  log(s"Sequential result - $seqRes")
  log(s"Parallel result - $parRes")

  def intersectionSize(a: GenSet[Int], b: GenSet[Int]): Int = {
    var count = 0
    for (x <- a) if (b contains x) count += 1

    count
  }

}

object ParSideEffectsCorrect extends App {

  val a = (0 until 1000).toSet
  val b = (0 until 1000 by 4).toSet

  val seqRes = intersectionSize(a, b)
  val parRes = intersectionSize(a.par, b.par)

  log(s"Sequential result - $seqRes")
  log(s"Parallel result - $parRes")

  def intersectionSize(a: GenSet[Int], b: GenSet[Int]): Int = {
    var count = new AtomicInteger(0)
    for (x <- a) if (b contains x) count.incrementAndGet()

    count.get
  }

}

object ParNonDeterministicOperation extends App {

  htmlSpec().foreach { lines =>
    val pattern = ".*TEXTAREA.*"

    val seqRes = lines.find(_ matches pattern)
    val parRes = lines.par.find(_ matches pattern)

    log(s"Sequential result - $seqRes")
    log(s"Parallel result - $parRes")
  }

  Thread sleep 7000

}

object ParNonCommutativeOperator extends App {

  import scala.collection.mutable

  val doc = mutable.ArrayBuffer.tabulate(20)(i => s"Page $i, ")

  test(doc)
  test(doc.toSet)

  def test(doc: Iterable[String]): Unit = {
    val seqRes = doc.toSeq.reduceLeft(_ + _)
    val parRes = doc.par.reduce(_ + _)

    log(s"Sequential result - $seqRes")
    log(s"Parallel result - $parRes")
  }

}

object ParNonAssociativeOperator extends App {

  test(0 until 30)

  def test(doc: Iterable[Int]): Unit = {
    val seqRes = doc.toSeq.reduceLeft(_ - _)
    val parRes = doc.par.reduce(_ - _)

    log(s"Sequential result - $seqRes")
    log(s"Parallel result - $parRes")
  }

}
