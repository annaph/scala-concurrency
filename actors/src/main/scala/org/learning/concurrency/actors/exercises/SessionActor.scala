package org.learning.concurrency.actors.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

object SessionActorApp extends App {

  val system = ActorSystem(name = "MyActorSystem")

  val testActor = system.actorOf(TestActor.props, name = "testActor")
  val sessionActor = system.actorOf(SessionActor.props(password = "123", testActor))

  sessionActor ! "Test1"
  sessionActor ! SessionActor.Start("123")
  sessionActor ! "Test2"
  sessionActor ! "Test3"
  sessionActor ! SessionActor.End
  sessionActor ! "Test4"

  Thread sleep (1000 * 3)

  system.terminate()
  Thread sleep 3000

}

class SessionActor(password: String, actor: ActorRef) extends Actor {

  import SessionActor._

  private val _log = Logging(context.system, this)

  override def receive: Receive = {
    case Start(p) if p == password =>
      context become working
      _log info "session started"
    case msg =>
      _log info s"Cannot forward '$msg'. Start the session!"
  }

  private def working: Receive = {
    case End =>
      context.become(receive)
      _log info "session ended"
    case msg =>
      actor forward msg
  }

}

object SessionActor {

  def props(password: String, actor: ActorRef): Props =
    Props(new SessionActor(password, actor))

  case class Start(password: String)

  case object End

}

class TestActor extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Receive = {
    case msg =>
      _log info msg.toString
  }

}

object TestActor {

  def props: Props =
    Props[TestActor]()

}
