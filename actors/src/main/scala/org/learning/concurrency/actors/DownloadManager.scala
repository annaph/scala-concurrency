package org.learning.concurrency.actors

import java.io.File
import java.net.MalformedURLException
import java.nio.charset.Charset

import akka.actor.SupervisorStrategy.{Escalate, Resume}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source

object DownloadManagerApp extends App {

  import DownloadManager._

  val downloadManager = ourSystem.actorOf(Props(classOf[DownloadManager], 4), name = "downloadManager")

  downloadManager ! Download("https://www.w3.org/Addressing/URL/url-spec.txt", "url-spec.txt")
  downloadManager ! Download("invalid-url", "invalid.txt")
  downloadManager ! Download("https://github.com/scala/scala/blob/master/README.md", "scala-README.md")
  Thread sleep 3000

  ourSystem.terminate()
  Thread sleep 3000

}

class DownloadManager(val downloadSlots: Int) extends Actor {

  import DownloadManager._

  private val _log = Logging(context.system, this)

  private val _pendingRequests = mutable.Queue.empty[Download]

  private val _workers = mutable.Queue.empty[ActorRef]

  private val _workItems = mutable.Map.empty[ActorRef, Download]

  override def receive: Actor.Receive = {
    case request: Download =>
      _pendingRequests enqueue request
      initiateDownload()
    case Finished(dest) =>
      _workItems.remove(sender()) match {
        case Some(_) =>
          _workers enqueue sender()
        case None =>
      }

      _log info s"Download to '$dest' finished, ${_workers.size} downloads slots left"
      initiateDownload()
  }

  private def initiateDownload(): Unit =
    if (_pendingRequests.nonEmpty && _workers.nonEmpty) {
      val request = _pendingRequests.dequeue()
      val worker = _workers.dequeue()

      _log info s"workItem starting, ${_workers.size} download slots left"
      _workItems.put(worker, request)

      worker ! request
    }

  override def preStart(): Unit = {
    _log info "about to start DownloadManager..."
    for (i <- 0 until downloadSlots)
      _workers enqueue context.actorOf(Props[Downloader](), name = s"downloader-$i")
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 7, withinTimeRange = 1.seconds) {
      case e: MalformedURLException =>
        _workers enqueue sender()
        _workItems remove sender()

        _log info s"Resource could not be found: $e"
        Resume
      case _ =>
        Escalate
    }

}

object DownloadManager {

  case class Download(url: String, dest: String)

  case class Finished(dest: String)

}

class Downloader extends Actor {

  override def receive: Actor.Receive = {
    case DownloadManager.Download(url, dest) =>
      val parent = sender()
      val src = Source fromURL url
      FileUtils.write(new File(dest), src.mkString, Charset forName "UTF-8")
      parent ! DownloadManager.Finished(dest)
      src.close()
  }

}
