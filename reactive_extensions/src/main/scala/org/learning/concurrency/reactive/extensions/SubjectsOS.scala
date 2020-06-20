package org.learning.concurrency.reactive.extensions

import io.reactivex.rxjava3.disposables.Disposable
import org.learning.concurrency.reactive.extensions.ObservablesDisposable.modifiedFiles

import scala.concurrent.duration._

object SubjectsOS extends App {

  log("RxOS booting...")

  val modules: List[Observable[String]] = List(
    TimeModule.systemClock,
    FileSystemModule.fileModifications
  )

  val loadedModules: List[Disposable] = modules.map(_ subscribeWith Subject.toDisposableObserver(RxOS.messageBus))

  log("RxOs boot seque5nce finished!")

  Thread sleep 12000

  loadedModules.foreach(_.dispose())

  log("RxOS dumping the complete log event")
  RxOS.messageLog subscribe log _

  log("RxOS going for shutdown")

  object RxOS {
    val messageBus: Subject[String] = Subject.publishSubject()
    messageBus subscribe log _

    val messageLog: Subject[String] = Subject.replaySubject()
    messageBus subscribe messageLog
  }

  object TimeModule {
    val systemClock: Observable[String] = Observable.interval(1.seconds)
      .map(t => s"systime: $t")
  }

  object FileSystemModule {
    val fileModifications: Observable[String] = modifiedFiles(".")
      .map(filename => s"file modification: $filename")
  }

}
