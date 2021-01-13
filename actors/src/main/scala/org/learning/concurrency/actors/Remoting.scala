package org.learning.concurrency.actors

import akka.actor.{Actor, ActorIdentity, Identify, Props}
import akka.event.Logging

object RemotingPongySystem extends App {

  val system = remotingSystem(name = "PongyDimension", port = 4000)
  val pongy = system.actorOf(Props[Pongy](), name = "pongy")

  Thread sleep (1000 * 61)

  system.terminate()
  Thread sleep 3000

}

object RemotingPingySystem extends App {

  val system = remotingSystem(name = "PingyDimension", port = 4001)
  val runner = system.actorOf(Props[Runner](), name = "runner")

  runner ! "start"
  Thread sleep (1000 * 61)

  system.terminate()
  Thread sleep 3000

}

class Runner extends Actor {

  private val _log = Logging(context.system, this)

  private val _pingy = context.actorOf(Props[Pingy](), name = "pingy")

  override def receive: Actor.Receive = {
    case "start" =>
      val path = context actorSelection "akka://PongyDimension@127.0.0.1:4000/user/pongy"
      path ! Identify(0)
    case ActorIdentity(0, Some(ref)) =>
      _pingy ! ref
    case ActorIdentity(0, None) =>
      _log info "Something is wrong -- no pongy anywhere!"
      context stop self
    case "pong" =>
      _log info "got a pong from another dimension."
      context stop self
  }

}
