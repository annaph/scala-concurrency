package org.learning.concurrency.reactive.extensions

import java.io.File

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationMonitor, FileAlterationObserver}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ObservablesItems extends App {

  val observable = Observable.items("Pascal", "Java", "Scala")

  observable.subscribe(name => log(s"learned the $name language"))
  observable.subscribe(name => log(s"forgot the $name language"))

}

object ObservablesTimer extends App {

  val observable = Observable.timer(1.second)

  observable.subscribe(_ => log("Timeout!"))
  observable.subscribe(_ => log("Another timeout!"))

  Thread sleep 3000

}

object ObservablesExceptions extends App {

  val observable1 = Observable.items(1, 2)
  val observable2 = Observable error new RuntimeException
  val observable3 = Observable.items(3, 4)

  val observable = observable1 ++ observable2 ++ observable3

  observable.subscribe(
    n => log(s"number $n"),
    e => log(s"an error occurred: $e"))

}

object ObservablesLifetime extends App {

  val classics = List("Il buono, il brutto, il cattivo.", "Back to the future", "Die Hard")
  val observable = Observable from classics

  val observer = new Observer[String] {

    override def onNext(classic: String): Unit =
      log(s"Movies Watchlist - $classic")

    override def onError(e: Throwable): Unit =
      log(s"Ooops - $e!")

    override def onComplete(): Unit =
      log("No more movies.")

    override def onSubscribe(d: Disposable): Unit =
      log("Subscribed to observable.")

  }

  observable subscribe observer

}

object ObservablesCreate extends App {

  val observable = Observable.create[String] { emitter =>
    emitter onNext "JVM"
    emitter onNext ".NET"
    emitter onNext "DartVM"

    emitter.onComplete()
  }

  log("About to subscribe")

  observable.subscribe(
    log,
    e => log(s"oops - $e"),
    log("Done!"))

}

object ObservablesCreateFuture extends App {

  val f = Future {
    Thread sleep 1000
    log("future completing...")
    "Back to the Future(s)"
  }

  val observable = Observable.create[String] { emitter =>
    f.onComplete {
      case Success(value) =>
        emitter onNext value
        emitter.onComplete()
      case Failure(e) =>
        emitter onError e
    }
  }

  Thread sleep 3000

  observable subscribe log _

  Thread sleep 7000

}

object ObservablesFromFuture extends App {

  val future = Future {
    Thread sleep 3000
    "Back to the Future(s)"
  }

  val observable = Observable from future

  observable subscribe log _

  Thread sleep 7000

}

object ObservablesDisposable extends App {

  val directory = "."

  log("starting to monitor files")
  val disposable = modifiedFiles(directory).subscribe(filename => log(s"$filename modified!"))
  log("please, modify and save a file")

  Thread sleep 31000

  disposable.dispose()
  disposable.dispose()
  log("monitoring done")

  def modifiedFiles(directory: String): Observable[String] = {
    Observable.create[String] { emitter =>
      val fileMonitor = new FileAlterationMonitor(1000)
      val fileObserver = new FileAlterationObserver(directory)

      val fileListener = new FileAlterationListenerAdaptor {
        override def onFileChange(file: File): Unit =
          emitter onNext file.getName
      }

      fileObserver addListener fileListener
      fileMonitor addObserver fileObserver

      emitter setDisposable new FileMonitorDisposable(fileMonitor)

      fileMonitor.start()
    }
  }

  class FileMonitorDisposable(fileMonitor: FileAlterationMonitor) extends Disposable {

    private var _disposed = false

    override def dispose(): Unit = {
      log("Disposing...")
      fileMonitor.stop()

      _disposed = true
    }


    override def isDisposed: Boolean = _disposed

  }

}

object ObservablesHot extends App {

  val directory = "."

  val fileMonitor = new FileAlterationMonitor(1000)
  fileMonitor.start()

  log("first disposable call")
  val firstDisposable = modifiedFilesObservable(directory).subscribe(filename => log(s"$filename modified!"))

  Thread sleep 7000

  log("another disposable call")
  val secondDisposable = modifiedFilesObservable(directory).subscribe(filename => log(s"$filename changed!"))

  Thread sleep 7000

  log("dispose second disposable")
  secondDisposable.dispose()

  Thread sleep 7000

  fileMonitor.stop()

  def modifiedFilesObservable(directory: String): Observable[String] = {
    val fileObserver = new FileAlterationObserver(directory)
    fileMonitor addObserver fileObserver

    Observable.create[String] { emitter =>
      val fileListener = new FileAlterationListenerAdaptor {
        override def onFileChange(file: File): Unit =
          emitter onNext file.getName
      }

      fileObserver addListener fileListener
      emitter setDisposable new FileMonitorDisposable(fileObserver, fileListener)
    }
  }

  class FileMonitorDisposable(fileObserver: FileAlterationObserver,
                              fileListener: FileAlterationListenerAdaptor) extends Disposable {

    private var _disposed = false

    override def dispose(): Unit = {
      log("Disposing...")
      fileObserver removeListener fileListener

      _disposed = true
    }

    override def isDisposed: Boolean = _disposed

  }

}
