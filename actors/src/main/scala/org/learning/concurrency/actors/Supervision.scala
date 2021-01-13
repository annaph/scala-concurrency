package org.learning.concurrency.actors

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorKilledException, Kill, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging

object SupervisionKill extends App {

  val supervisor = ourSystem.actorOf(Props[Supervisor](), name = "supervisor")

  ourSystem.actorSelection("/user/supervisor/*") ! Kill
  ourSystem.actorSelection("/user/supervisor/*") ! "sorry about that"
  ourSystem.actorSelection("/user/supervisor/*") ! "kaboom".toList
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class Supervisor extends Actor {

  private val _log = Logging(context.system, this)

  context.actorOf(Props[Naughty](), name = "victim")

  override def receive: Actor.Receive =
    PartialFunction.empty

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException =>
      Restart
    case _ =>
      Escalate
  }

  override def preStart(): Unit =
    _log info "about to start supervisor..."

  override def postRestart(reason: Throwable): Unit = {
    _log info "supervisor to be restarted"
    super.postRestart(reason)
  }

  override def postStop(): Unit =
    _log info "supervisor stopped"
}

class Naughty extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Actor.Receive = {
    case str: String =>
      _log info str
    case _ =>
      throw new RuntimeException
  }

  override def preStart(): Unit =
    _log info "about to start naugthy..."

  override def postRestart(reason: Throwable): Unit = {
    _log info "naugthy to be restarted"
    super.postRestart(reason)
  }

  override def postStop(): Unit =
    _log info "naugthy stopped"

}
