package org.learning.concurrency.ftp.client

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props}
import akka.event.Logging
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.learning.concurrency.ftp.client.FtpClientActor.Start
import org.learning.concurrency.ftp.server.FtpServerActor

import scala.concurrent.ExecutionContext.Implicits.global

class FtpClientActor(implicit timeout: Timeout) extends Actor {

  private val logger = Logging(context.system, this)

  override def receive: Receive = unconnected

  private def unconnected: Receive = {
    case Start(actorServerUrl) =>
      val serverActorPath = s"akka://FTPServerSystem@$actorServerUrl/user/FTPServerActor"
      val serverActor = context actorSelection serverActorPath
      serverActor ! Identify(())
      context become connecting(sender())
  }

  private def connecting(clientApp: ActorRef): Receive = {
    case ActorIdentity(_, Some(serverActorRef)) =>
      logger info s"Found server actor: $serverActorRef"
      clientApp ! true
      context become connected(serverActorRef)

    case ActorIdentity(_, None) =>
      logger warning "Could not find server actor"
      clientApp ! false
      context become unconnected

  }

  private def connected(serverActor: ActorRef): Receive = {
    case command: FtpServerActor.Command =>
      val result = serverActor ? command
      result pipeTo sender()
  }

}

object FtpClientActor {

  def apply(implicit timeout: Timeout): Props =
    Props(new FtpClientActor)

  case class Start(serverActorUrl: String)

}
