package org.learning.concurrency.promises

import java.io.File
import java.util.{Timer, TimerTask}

import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationMonitor, FileAlterationObserver}
import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{CancellationException, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object PromisesCreate extends App {

  val p = Promise[String]
  val q = Promise[String]

  p.future.foreach { text =>
    log(s"Promise p succeed with '$text'")
  }

  Thread sleep 1000

  p success "kept"

  val secondAttempt = p trySuccess "kept again"

  log(s"Second attempt to complete the same promise went well? $secondAttempt")

  q failure new Exception("not kept")

  q.future.failed.foreach { e =>
    log(s"Promise q failed with $e")
  }

  Thread sleep 3000

}

object PromisesCustomAsync extends App {

  val future = myFuture {
    "naaa" + "na" * 8 + " Katamari Damacy!"
  }

  def myFuture[T](body: => T): Future[T] = {
    val p = Promise[T]

    global.execute { () =>
      Try(body) match {
        case Success(result) =>
          p success result
        case Failure(NonFatal(e)) =>
          p failure e
      }
    }

    p.future
  }

  future foreach log

  Thread sleep 3000

}

object PromisesAndCallbacks extends App {

  def fileCreated(directory: String): Future[String] = {
    val p = Promise[String]

    val fileMonitor = new FileAlterationMonitor(1000)
    val observer = new FileAlterationObserver(directory)

    val listener = new FileAlterationListenerAdaptor {

      override def onFileCreate(file: File): Unit = {
        p trySuccess file.getName
        fileMonitor.stop()
      }

    }

    observer addListener listener
    fileMonitor addObserver observer

    fileMonitor.start()

    p.future
  }

  fileCreated(".").foreach { filename =>
    log(s"Detected new file '$filename'")
  }

  Thread sleep 37000

}

object PromisesAndCustomOperations extends App {

  implicit class FutureOps[T](self: Future[T]) {

    def or(that: Future[T]): Future[T] = {
      val p = Promise[T]

      self onComplete p.tryComplete
      that onComplete p.tryComplete

      p.future
    }
  }

  val f = Future("now") or Future("later")

  f.foreach { msg =>
    log(s"The future is $msg")
  }

  Thread sleep 3000

}

object PromisesAndTimers extends App {

  import PromisesAndCustomOperations.FutureOps

  val timer = new Timer(true)

  val f = timeout(1000).map(_ => "timeout!") or Future {
    Thread sleep 907
    "work completed!"
  }

  def timeout(millis: Long): Future[Unit] = {
    val p = Promise[Unit]

    val task = new TimerTask {
      override def run(): Unit = {
        p success()
        timer.cancel()
      }
    }

    timer.schedule(task, millis)

    p.future
  }

  f foreach log

  Thread sleep 3000

}

object PromisesCancellation extends App {

  type Cancellable[T] = (Promise[Unit], Future[T])

  val (cancel, value) = cancellable { cancel =>
    for (i <- 0 until 5) {
      if (cancel.isCompleted) throw new CancellationException

      Thread sleep 500
      log(s"$i: working")
    }

    "resulting value"
  }

  def cancellable[T](b: Future[Unit] => T): Cancellable[T] = {
    val p = Promise[Unit]

    val f = Future {
      val result = b(p.future)
      if (!p.tryFailure(new Exception))
        throw new CancellationException

      result
    }

    p -> f
  }

  Thread sleep 1500
  cancel trySuccess ()

  log("computation cancelled")
  value.failed.foreach { e =>
    log(s"Exception occurred: $e")
  }

  Thread sleep 3000

}
