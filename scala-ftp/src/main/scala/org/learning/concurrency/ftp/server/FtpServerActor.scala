package org.learning.concurrency.ftp.server

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import org.learning.concurrency.ftp.server.FtpServerActor.{CopyFile, DeleteFile, GetFileList}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FtpServerActor(fileSystem: FileSystem) extends Actor {

  override def receive: Receive = {
    case GetFileList(dir) =>
      sender() ! fileSystem.fileList(dir).values.toSeq

    case CopyFile(srcPath, destPath) =>
      val result = Future {
        Try(fileSystem.copyFile(srcPath, destPath))
      }
      result pipeTo sender()

    case DeleteFile(srcPath) =>
      val result = Future {
        Try(fileSystem deleteFile srcPath)
      }
      result pipeTo sender()
  }

}

object FtpServerActor {

  def apply(fileSystem: FileSystem): Props =
    Props(new FtpServerActor(fileSystem))

  sealed trait Command

  case class GetFileList(dir: String) extends Command

  case class CopyFile(srcPath: String, destPath: String) extends Command

  case class DeleteFile(srcPath: String) extends Command

}
