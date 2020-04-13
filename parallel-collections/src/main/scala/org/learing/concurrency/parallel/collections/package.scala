package org.learing.concurrency.parallel

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

package object collections {

  @volatile private var _dummy: Any = _

  def log(msg: String): Unit = {
    println(s"${Thread.currentThread.getName}: $msg")
  }

  def warmedTimed[T](times: Int = 100)(body: => T): Double = {
    for (_ <- 0 until times) body
    timed(body)
  }

  def timed[T](body: => T): Double = {
    val start = System.nanoTime()
    _dummy = body
    val end = System.nanoTime()

    ((end - start) / 1000) / 1000.0
  }

  def htmlSpec()(implicit ec: ExecutionContext): Future[Seq[String]] = Future {
    val f = Source fromURL "https://www.w3.org/MarkUp/html-spec/html-spec.txt"
    try f.getLines().toSeq finally f.close()
  }

  def urlSpec()(implicit ec: ExecutionContext): Future[Seq[String]] = Future {
    val f = Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt")
    try f.getLines.toList finally f.close()
  }

}
