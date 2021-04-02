package org.learning.concurrency.ftp.client

import akka.pattern.ask
import akka.util.Timeout
import org.learning.concurrency.ftp.remotingSystem
import org.learning.concurrency.ftp.server.{FileInfo, FtpServerActor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait FtpClientApi {

  private implicit val timeout: Timeout = Timeout(3.seconds)

  private val actorSystem = remotingSystem(name = "FTPClientSystem", port = 0)

  private val clientActor = actorSystem.actorOf(FtpClientActor(timeout), name = "FTPClientActor")

  def serverHost: String

  lazy val connected: Future[Boolean] = {
    val result = clientActor ? FtpClientActor.Start(serverHost)
    result.mapTo[Boolean]
  }

  def fileList(dir: String): Future[(String, Seq[FileInfo])] = {
    val result = clientActor ? FtpServerActor.GetFileList(dir)
    result.mapTo[Seq[FileInfo]].map(dir -> _)
  }

  def copyFile(srcPath: String, destPath: String): Future[String] = {
    val result = clientActor ? FtpServerActor.CopyFile(srcPath, destPath)
    result.mapTo[Try[String]].map {
      case Success(path) =>
        path
      case Failure(e) =>
        throw e
    }
  }

  def deleteFile(srcPath: String): Future[String] = {
    val result = clientActor ? FtpServerActor.DeleteFile(srcPath)
    result.mapTo[Try[String]].map {
      case Success(path) =>
        path
      case Failure(e) =>
        throw e
    }
  }

  def stopClientApi(): Future[Unit] = {
    actorSystem stop clientActor
    actorSystem.terminate().map(_ => ())
  }

}
