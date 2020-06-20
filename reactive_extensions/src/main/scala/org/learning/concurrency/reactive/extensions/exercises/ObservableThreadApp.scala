package org.learning.concurrency.reactive.extensions.exercises

import java.time.LocalDateTime

import org.learning.concurrency.reactive.extensions.{Observable, log}

import scala.annotation.tailrec
import scala.concurrent.duration._

object ObservableThreadApp extends App {

  val threads: Observable[Thread] =
    for {
      _ <- Observable.interval(1.seconds)
      thread <- ThreadHelper.newThreads
    } yield thread

  threads.subscribe { thread =>
    log(s"${LocalDateTime.now().toString}: ${thread.toString}")
  }

  Thread sleep 3000
  createThread("A")

  Thread sleep 2000
  createThread("B")

  Thread sleep 1000
  createThread("C")

  Thread sleep 7000

  def createThread(name: String): Unit = {
    val t = new Thread(name) {
      override def run(): Unit = Thread sleep 5000
    }

    t.start()
  }

}

object ThreadHelper {

  private var _existingThreads: Set[Thread] = Set.empty

  def newThreads: Observable[Thread] = {
    val currentThreads = allCurrentThreads
    val newlyThreads = currentThreads.filter(!_existingThreads.contains(_))
    _existingThreads = currentThreads.toSet

    Observable.create { emitter =>
      newlyThreads foreach emitter.onNext
    }
  }

  private def allCurrentThreads: Array[Thread] = {
    val root = rootThreadGroup
    val threads = new Array[Thread](root.activeCount())

    root.enumerate(threads, true)
    threads.filter(_ != null)
  }

  private def rootThreadGroup: ThreadGroup = {
    @tailrec
    def go(threadGroup: ThreadGroup): ThreadGroup = {
      val parent = threadGroup.getParent
      if (parent == null) threadGroup else go(parent)
    }

    go(Thread.currentThread.getThreadGroup)
  }

}