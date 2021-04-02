package org.learning.concurrency.ftp.server

import org.learning.concurrency.ftp.Implicits._
import org.learning.concurrency.ftp.remotingSystem

import java.io.File

object FtpServerApp extends App {

  // Create File System
  val fileSystem = FileSystem(rootPath = ".")
  fileSystem.init()

  // Monitor File System
  val fileSystemMonitor = FileSystemMonitor(rootPath = ".")

  fileSystemMonitor.fileSystemEvents.subscribeObserver {
    case FileEvent.Created(path) =>
      fileSystem addOrUpdateFileInfo FileInfo(new File(path))
    case FileEvent.Modified(path) =>
      fileSystem addOrUpdateFileInfo FileInfo(new File(path))
    case FileEvent.Deleted(path) =>
      fileSystem removeFileInfo path
  }

  // Create Actor System
  val actorSystem = remotingSystem(name = "FTPServerSystem", port = args(0).toInt)
  actorSystem.actorOf(FtpServerActor(fileSystem), name = "FTPServerActor")

}
