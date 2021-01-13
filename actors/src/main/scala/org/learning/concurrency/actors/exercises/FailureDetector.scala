package org.learning.concurrency.actors.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Identify, Props}
import akka.event.Logging
import akka.pattern._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object FailureDetectorApp extends App {

  val system = ActorSystem("FailureDetectorSystem")

  val parentActor = system.actorOf(ParentActor.props, name = "ParentActor")

  Thread sleep 7000
  parentActor ! ParentActor.StopChildActor
  Thread sleep 7000

  system.terminate()
  Thread sleep 3000

}

class ParentActor extends Actor {

  private val _log = Logging(context.system, this)

  private val _childActor =
    context.actorOf(ChildActor.props, name = "ChildActor")

  private val _failureDetectorActor =
    context.actorOf(FailureDetector.props(actorRef = _childActor, interval = 1, threshold = 2), name = "FailureDetector")

  override def receive: Receive = {
    case ParentActor.StopChildActor =>
      _log info "Stop Child actor"
      context.stop(_childActor)
    case ParentActor.Failed(actorRef) =>
      _log info s"Parent. $actorRef not reply !!!"
      _failureDetectorActor ! FailureDetector.StopChecking
  }

}

object ParentActor {

  def props: Props =
    Props[ParentActor]()

  case class Failed(actorRef: ActorRef)

  case object StopChildActor

}

class ChildActor extends Actor {

  override def receive: Receive = PartialFunction.empty

}

object ChildActor {

  def props: Props =
    Props[ChildActor]()

}

class FailureDetector(actorRef: ActorRef, interval: Int, threshold: Int) extends Actor {

  private val _log = Logging(context.system, this)

  private val _cancelable =
    context.system.scheduler.scheduleAtFixedRate(
      initialDelay = 0.second,
      interval = interval.second,
      receiver = self,
      message = FailureDetector.Check)

  override def receive: Receive = {
    case FailureDetector.Check =>
      _log info "sending Identify message"
      (actorRef ? Identify(actorRef.path)) (threshold.second).onComplete {
        case Success(_) =>
          _log info "OK"
        case Failure(e) =>
          _log error s"No reply: ${e.getMessage}"
          context.actorSelection(actorRef.path.parent) ! ParentActor.Failed(actorRef)
      }
    case FailureDetector.StopChecking =>
      _log info "Stop checking"
      _cancelable.cancel()
  }

}

object FailureDetector {

  def props(actorRef: ActorRef, interval: Int, threshold: Int): Props =
    Props(new FailureDetector(actorRef, interval, threshold))

  case object Check

  case object StopChecking

}
