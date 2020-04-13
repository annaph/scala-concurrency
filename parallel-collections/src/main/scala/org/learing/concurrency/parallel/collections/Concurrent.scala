package org.learing.concurrency.parallel.collections

import java.util.concurrent.ConcurrentSkipListSet

import scala.collection.mutable
import scala.collection.parallel.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object ConcurrentCommon {

  def run(intersection: (Set[String], Set[String]) => mutable.Set[String]): Unit = {
    val future = for {
      htmlSpec <- htmlSpec()
      urlSpec <- urlSpec()
    } yield {
      val a = htmlSpec.mkString.split("\\s").toSet
      val b = urlSpec.mkString.split("\\s").toSet

      intersection(a, b)
    }

    future.onComplete {
      case Success(result) =>
        log(s"Result: $result")
      case Failure(e) =>
        throw new Exception("Error finding intersection!", e)
    }

    Thread sleep 7000
  }

}

object ConcurrentWrong extends App {

  ConcurrentCommon.run(intersection)

  def intersection(a: Set[String], b: Set[String]): mutable.Set[String] = {
    val result = mutable.HashSet.empty[String]

    for {
      x <- a.par
      if b contains x
    } result add x

    result
  }

}

object ConcurrentCollections extends App {

  import scala.jdk.CollectionConverters._

  ConcurrentCommon.run(intersection)

  def intersection(a: Set[String], b: Set[String]): mutable.Set[String] = {
    val result = new ConcurrentSkipListSet[String]()

    for {
      x <- a.par
      if b contains x
    } result add x

    result.asScala
  }

}

object ConcurrentTrieMap extends App {

  import scala.collection.concurrent

  val cache = concurrent.TrieMap.empty[Int, String]
  for (i <- 0 until 100) cache.put(i, i.toString)

  for ((number, str) <- cache.par) cache.put(-number, s"-$str")

  log(s"cache - ${cache.keys.toList.sorted}")

}
