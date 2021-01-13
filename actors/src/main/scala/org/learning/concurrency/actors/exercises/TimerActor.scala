package org.learning.concurrency.actors.exercises

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern._
import org.learning.concurrency.actors.exercises.RunnerActor.Register
import org.learning.concurrency.actors.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

object TimerActorApp extends App {

  val system = ActorSystem(name = "MyActorSystem")

  val timerActor = system.actorOf(TimerActor.props, name = "timer")

  val runnerActors = for (i <- 1 to 12) yield
    system.actorOf(RunnerActor.props, name = s"runner-$i")

  runnerActors.zipWithIndex.foreach {
    case (actor, i) =>
      actor ! RunnerActor.Register((i + 1) * 1000)
  }

  Thread sleep (17 * 1000)

  system.terminate()
  Thread sleep 3000

}

class RunnerActor extends Actor {

  private val _timerActor = context.actorSelection(path = "/user/timer")

  override def receive: Receive = {
    case msg: Register =>
      _timerActor ! msg
    case TimerActor.Timeout =>
      val mySender = sender()
      log(msg = s"Timeout message from $mySender")
  }

}

object RunnerActor {

  def props: Props = Props[RunnerActor]()

  case class Register(timeout: Long)

}

class TimerActor extends Actor {

  import TimerActor._

  override def receive: Receive = {
    case RunnerActor.Register(timeout) =>
      val mySender = sender()
      log(msg = s"Register message (timeout = $timeout) from $mySender")

      val result = waitFor(timeout).map(_ => Timeout)
      pipe(result) to mySender
  }

  private def waitFor(period: Long): Future[Unit] = Future {
    blocking {
      Thread sleep period
    }
  }

}

object TimerActor {

  def props: Props = Props[TimerActor]()

  case object Timeout

}
