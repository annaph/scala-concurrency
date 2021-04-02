package org.learning.concurrency.ftp.server

import io.reactivex.rxjava3.core.{Observable, ObservableEmitter}
import io.reactivex.rxjava3.disposables.Disposable
import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationMonitor, FileAlterationObserver}
import org.learning.concurrency.ftp.server.FileSystemMonitor.{FileListener, FileMonitorDisposable}

import java.io.File

class FileSystemMonitor(rootPath: String) {

  def fileSystemEvents: Observable[FileEvent] = Observable.create { emitter =>
    val fileMonitor = new FileAlterationMonitor(1000)
    val fileObserver = new FileAlterationObserver(rootPath)
    val fileListener = new FileListener(emitter)
    val fileMonitorDisposable = new FileMonitorDisposable(fileMonitor)

    fileObserver addListener fileListener
    fileMonitor addObserver fileObserver
    emitter setDisposable fileMonitorDisposable

    fileMonitor.start()
  }

}

object FileSystemMonitor {

  def apply(rootPath: String): FileSystemMonitor =
    new FileSystemMonitor(rootPath)

  class FileListener(emitter: ObservableEmitter[FileEvent]) extends FileAlterationListenerAdaptor {

    override def onFileCreate(file: File): Unit =
      emitter onNext FileEvent.Created(file.getPath)

    override def onFileChange(file: File): Unit =
      emitter onNext FileEvent.Modified(file.getPath)

    override def onFileDelete(file: File): Unit =
      emitter onNext FileEvent.Deleted(file.getPath)

    override def onDirectoryCreate(directory: File): Unit =
      emitter onNext FileEvent.Created(directory.getPath)

    override def onDirectoryChange(directory: File): Unit =
      emitter onNext FileEvent.Modified(directory.getPath)

    override def onDirectoryDelete(directory: File): Unit =
      emitter onNext FileEvent.Deleted(directory.getPath)

  }

  class FileMonitorDisposable(fileMonitor: FileAlterationMonitor) extends Disposable {

    private var disposed = false

    override def dispose(): Unit = {
      fileMonitor.stop()
      disposed = true
    }

    override def isDisposed: Boolean = disposed

  }

}
