package org.learning.concurrency.actors.exercises

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging
import org.learning.concurrency.actors.log

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object ActorExecutionContextApp extends App {

  val executionContext = ActorExecutionContext()

  executionContext.execute(() => log(msg = "run"))

  executionContext.execute(() => {
    log(msg = "run (exception)")
    throw new Exception("test exception")
  })

  Thread sleep 3000

  executionContext.shutdown()
  Thread sleep 3000

}

class ActorExecutionContext(actorSystemName: String) extends ExecutionContext {

  private val _actorSystem = ActorSystem(actorSystemName)

  private val _executorActor = _actorSystem.actorOf(ExecutorActor props this)

  override def execute(runnable: Runnable): Unit =
    _executorActor ! ExecutorActor.Execute(runnable)

  override def reportFailure(cause: Throwable): Unit =
    log(msg = s"error: ${cause.getMessage}")

  def shutdown(): Unit =
    _actorSystem.terminate()

}

object ActorExecutionContext {

  def apply(): ActorExecutionContext =
    new ActorExecutionContext(actorSystemName = "ExecutionContextActorSystem")

}

class ExecutorActor(actorExecutionContext: ActorExecutionContext) extends Actor {

  import ExecutorActor._

  private val _log = Logging(context.system, this)

  override def receive: Receive = {
    case Execute(runnable) =>
      Try(runnable.run()) match {
        case Success(_) =>
          _log info "Result OK"
        case Failure(e) =>
          actorExecutionContext reportFailure e
      }
  }

}

object ExecutorActor {

  def props(actorExecutionContext: ActorExecutionContext): Props =
    Props(new ExecutorActor(actorExecutionContext))

  case class Execute(runnable: Runnable)

}
