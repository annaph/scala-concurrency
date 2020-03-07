package org.learning.concurrency.futures.exercises

import java.util.{Timer, TimerTask}

import org.learning.concurrency.futures.exercises.ReadUrlWithTimeout.{printDots, readUrl, timeout}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.io.{Source, StdIn}

object ReadUrlWithTimeoutApp extends App {

  println("---------------------------------------------")
  println("Please, input URL")

  val url = StdIn.readLine().trim
  println(s"URL: $url")

  val p = Promise[String]

  readUrl(url, p)
  printDots(url, p)
  timeout(2000, p)

  p.future.foreach { msg =>
    println()
    println(msg)
  }

  Thread sleep 12000

}

object ReadUrlWithTimeout {

  private val _timer = new Timer(true)

  def readUrl(url: String, p: Promise[String]): Future[Unit] = Future {
    val f = Source fromURL url
    try f.getLines() mkString "\n" finally f.close()
  }.map { msg =>
    p trySuccess msg
    ()
  }.recover { e =>
    p trySuccess s"Error!!! ${e.toString}"
    ()
  }

  def printDots(url: String, p: Promise[String]): Future[Unit] = Future {
    def go(): Unit = p match {
      case _ if p.isCompleted =>
        println()
        ()
      case _ =>
        print(".")
        delay(50).foreach(_ => go())
    }

    println(s"Reading from '$url', please wait")
    go()
  }

  def timeout(millis: Long, p: Promise[String]): Future[Unit] = Future {
    delay(millis).foreach { _ =>
      p trySuccess s"Sorry, timed out ($millis ms)"
    }
  }

  private def delay(millis: Long): Future[Unit] = {
    val p = Promise[Unit]

    val task = new TimerTask {
      override def run(): Unit = p success()
    }

    _timer.schedule(task, millis)

    p.future
  }

}
