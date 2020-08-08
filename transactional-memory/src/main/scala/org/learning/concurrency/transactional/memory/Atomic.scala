package org.learning.concurrency.transactional.memory

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm._
import scala.util.{Failure, Random, Success}

object AtomicHistoryBad extends App {

  val urls = new AtomicReference[List[String]](Nil)
  val clen = new AtomicInteger(0)

  Future {
    addUrl("http://scala-lang.org")
    addUrl("https://github.com/scala/scala")
    addUrl("http://www.scala-lang.org/api")

    log("done browsing")
  }

  Future(log(s"sending: ${getUrlArray.mkString}")).onComplete {
    case Failure(e) =>
      log(s"problems getting the array $e")
    case Success(_) =>
  }

  Thread sleep 3000

  def addUrl(url: String): Unit = {
    @tailrec
    def append(): Unit = {
      val oldUrls = urls.get()
      val newUrls = url :: oldUrls
      if (!urls.compareAndSet(oldUrls, newUrls)) append()
    }

    append()
    if (Random.nextBoolean()) Thread sleep 12
    clen addAndGet url.length + 1
  }

  def getUrlArray: Array[Char] = {
    val arr = new Array[Char](clen.get)

    urls.get().map(_ + "\n").flatMap(_.toCharArray).zipWithIndex.foreach {
      case (ch, i) =>
        arr(i) = ch
    }

    arr
  }

}

object AtomicHistorySTM extends App {

  val urls = Ref[List[String]](Nil)
  val clen = Ref[Int](0)

  Future {
    addUrl("http://scala-lang.org")
    addUrl("https://github.com/scala/scala")
    addUrl("http://www.scala-lang.org/api")

    log("done browsing")
  }

  Thread sleep 31

  Future(log(s"sending: ${getUrlArray.mkString}")).onComplete {
    case Failure(e) =>
      log(s"problems getting the array $e")
    case Success(_) =>
  }

  Thread sleep 3000

  def addUrl(url: String): Unit = atomic { implicit txn =>
    val newUrls = url :: urls()
    urls update newUrls

    val newClen = clen() + url.length + 1
    clen update newClen
  }

  def getUrlArray: Array[Char] = atomic { implicit txn =>
    val arr = new Array[Char](clen())

    urls().map(_ + "\n").flatMap(_.toCharArray).zipWithIndex.foreach {
      case (ch, i) =>
        arr(i) = ch
    }

    arr
  }

}
