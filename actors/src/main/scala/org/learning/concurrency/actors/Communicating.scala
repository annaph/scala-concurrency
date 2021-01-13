package org.learning.concurrency.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object CommunicatingAsk extends App {

  val master = ourSystem.actorOf(Props[Master](), name = "master")

  master ! "start"
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class Master extends Actor {

  private val _log = Logging(context.system, this)

  private val _pingy = ourSystem.actorOf(Props[Pingy](), name = "pingy")

  private val _pongy = ourSystem.actorOf(Props[Pongy](), name = "pongy")

  override def receive: Actor.Receive = {
    case "start" =>
      _pingy ! _pongy
    case "pong" =>
      _log info "got a pong back!"
      context stop self
  }

  override def postStop(): Unit =
    _log info "master going down"

}

class Pingy extends Actor {

  private val _log = Logging(context.system, this)

  private implicit val _timeout: Timeout = Timeout(3.seconds)

  override def receive: Actor.Receive = {
    case pongyRef: ActorRef =>
      val mySender = sender()
      val f = pongyRef ? "ping"
      pipe(f) to mySender
  }

  override def postStop(): Unit =
    _log info "pingy going down"

}

class Pongy extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Actor.Receive = {
    case "ping" =>
      _log info "Got a ping -- ponging back"
      sender() ! "pong"
      context stop self
  }

  override def postStop(): Unit =
    _log info "pongy going down"

}

object CommunicatingRouter extends App {

  val router = ourSystem.actorOf(Props[Router](), name = "router")

  router ! "Hi"
  router ! "I'm talking to you"
  Thread sleep 1000

  router ! "stop"
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class Router extends Actor {

  private val _children = for (_ <- 0 until 4) yield context actorOf Props[StringPrinter]()

  private var _index = -1

  override def receive: Actor.Receive = {
    case "stop" =>
      context stop self
    case msg =>
      _index = (_index + 1) % 4
      _children(_index) forward msg
  }

}

class GracefulPingy extends Actor {

  private val _log = Logging(context.system, this)

  private val _pongy = context.actorOf(Props[Pongy](), name = "pongy")

  context watch _pongy

  override def receive: Actor.Receive = {
    case GracefulPingy.CustomShutdown =>
      context stop _pongy
    case Terminated(`_pongy`) =>
      context stop self
  }

  override def postStop(): Unit =
    _log info "pingy is down"

}

object GracefulPingy {

  case object CustomShutdown

}

object CommunicatingGracefulStop extends App {

  val pingy = ourSystem.actorOf(Props[GracefulPingy](), name = "pingy")

  gracefulStop(pingy, 3.seconds, GracefulPingy.CustomShutdown).onComplete {
    case Success(_) =>
      log("Graceful shutdown successful")
    case Failure(_) =>
      log("pingy not stopped!")
  }

  ourSystem.terminate()
  Thread sleep 3000

}
