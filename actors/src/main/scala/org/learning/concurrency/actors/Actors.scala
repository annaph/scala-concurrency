package org.learning.concurrency.actors

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props}
import akka.event.Logging

import scala.io.Source

class HelloActor(val hello: String) extends Actor {

  private val _log = Logging(this.context.system, this)

  override def receive: Receive = {
    case `hello` =>
      _log info s"Received a '$hello'... $hello!"
    case msg =>
      _log info s"Unexpected message '$msg'"
      this.context stop self
  }

}

object HelloActor {

  def props(hello: String): Props =
    Props(new HelloActor(hello))

  def propsAlt(hello: String): Props =
    Props(classOf[HelloActor], hello)

}

object ActorsCreate extends App {

  val hiActor: ActorRef = ourSystem.actorOf(HelloActor props "hi", name = "greeter")

  hiActor ! "hi"
  Thread sleep 1000

  hiActor ! "hola"
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class DeafActor extends Actor {

  private val _log = Logging(this.context.system, this)

  override def receive: Receive = PartialFunction.empty

  override def unhandled(message: Any): Unit = message match {
    case msg: String =>
      _log info s"could not handle '$msg'"
    case _ =>
      super.unhandled(message)
  }

}

object ActorsUnhandled extends App {

  val deafActor: ActorRef = ourSystem.actorOf(Props[DeafActor](), name = "deafy")

  deafActor ! "hi"
  Thread sleep 1000

  deafActor ! 1234
  Thread sleep 100

  ourSystem.terminate()
  Thread sleep 3000

}

class CountdownActor extends Actor {

  private val _log = Logging(this.context.system, this)

  private var _count = 12

  override def receive: Actor.Receive =
    counting

  private def counting: Actor.Receive = {
    case "count" =>
      _count -= 1
      _log info s"counter = ${_count}"
      if (_count == 0) this.context become done
  }

  private def done: Actor.Receive =
    PartialFunction.empty

}

object ActorsCountdown extends App {

  val countdownActor: ActorRef = ourSystem actorOf Props[CountdownActor]()

  for (_ <- 0 until 17) countdownActor ! "count"

  Thread sleep 3000

  ourSystem.terminate()
  Thread sleep 3000

}

class DictionaryActor extends Actor {

  import DictionaryActor._

  import scala.collection.mutable

  private val _log = Logging(this.context.system, this)

  private val _dictionary = mutable.Set.empty[String]

  override def receive: Actor.Receive =
    uninitialized

  override def unhandled(msg: Any): Unit =
    _log info s"message $msg should not be sent in this state!"

  private def uninitialized: Actor.Receive = {
    case Init(path) =>
      val in = this.getClass.getClassLoader getResourceAsStream path
      val words = Source fromInputStream in

      for (word <- words.getLines()) _dictionary += word
      this.context become initialized
  }

  private def initialized: Actor.Receive = {
    case IsWord(word) =>
      val exists = _dictionary contains word
      _log info s"word '$word' exists: $exists"
    case End =>
      _dictionary.clear()
      this.context become uninitialized
  }

}

object DictionaryActor {

  case class Init(path: String)

  case class IsWord(word: String)

  case object End

}

object ActorsBecome extends App {

  import DictionaryActor._

  val dictionaryActor: ActorRef = ourSystem.actorOf(Props[DictionaryActor](), name = "dictionary")

  dictionaryActor ! IsWord("program")
  Thread sleep 1000

  dictionaryActor ! Init("words.txt")
  Thread sleep 1000

  dictionaryActor ! IsWord("program")
  Thread sleep 1000

  dictionaryActor ! IsWord("balaban")
  Thread sleep 1000

  dictionaryActor ! End
  Thread sleep 1000

  dictionaryActor ! IsWord("termination")
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class ParentActor extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Actor.Receive = {
    case "create" =>
      context actorOf Props[ChildActor]()
      _log info s"create a new child - children = ${context.children}"
    case "sayhi" =>
      _log info "Kids, say hi!"
      for (child <- context.children) child ! "sayhi"
    case "stop" =>
      _log info "parent stopping"
      context stop self
  }

}

class ChildActor extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Actor.Receive = {
    case "sayhi" =>
      val parent = context.parent
      _log info s"my parent '$parent' made me say hi!"
  }

  override def postStop(): Unit =
    _log info "child stopped!"

}

object ActorsHierarchy extends App {

  val parent = ourSystem.actorOf(Props[ParentActor](), name = "parent")

  parent ! "create"
  parent ! "create"
  Thread sleep 1000

  parent ! "sayhi"

  parent ! "stop"
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class CheckActor extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Actor.Receive = {
    case path: String =>
      _log info s"checking path '$path'..."
      val selection = context actorSelection path
      selection ! Identify(path)
    case ActorIdentity(path, Some(ref)) =>
      _log info s"found actor '$ref' at path '$path'"
    case ActorIdentity(path, None) =>
      _log info s"could not find an actor at '$path'"
  }

}

object ActorsIdentify extends App {

  val checker: ActorRef = ourSystem.actorOf(Props[CheckActor](), name = "checker")

  checker ! "../*"
  Thread sleep 1000

  checker ! "../../*"
  Thread sleep 1000

  checker ! "/system/*"
  Thread sleep 1000

  checker ! "/user/checker2"
  Thread sleep 1000

  checker ! "akka://OurExampleSystem/system"
  Thread sleep 1000

  ourSystem stop checker
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}

class LifecycleActor extends Actor {

  private val _log = Logging(context.system, this)

  private var _child: ActorRef = _

  override def receive: Actor.Receive = {
    case num: Double =>
      _log info s"got a double - $num"
    case num: Int =>
      _log info s"got an integer - $num"
    case lst: List[_] if lst.nonEmpty =>
      _log info s"got a list - ${lst.head}, ..."
    case Nil =>
      throw new Exception("empty list!")
    case txt: String =>
      _child ! txt
  }

  override def preStart(): Unit = {
    _log info "about to start..."
    _child = context.actorOf(Props[StringPrinter](), name = "kiddo")
  }

  override def preRestart(reason: Throwable, msg: Option[Any]): Unit = {
    _log info s"about to restart because of '$reason', during message '$msg'"
    super.preRestart(reason, msg)
  }

  override def postRestart(reason: Throwable): Unit = {
    _log info s"just restarted due to '$reason'"
    super.postRestart(reason)
  }

  override def postStop(): Unit =
    _log info "just stopped!"

}

class StringPrinter extends Actor {

  private val _log = Logging(context.system, this)

  override def receive: Actor.Receive = {
    case msg =>
      _log info s"child got message '$msg'"
  }

  override def preStart(): Unit =
    _log info "child about to start..."

  override def postStop(): Unit =
    _log info "child just stopped!"

}

object ActorsLifecycle extends App {

  val testy: ActorRef = ourSystem.actorOf(Props[LifecycleActor](), name = "testy")

  testy ! math.Pi
  Thread sleep 1000

  testy ! 7
  Thread sleep 1000

  testy ! List("Anna")
  Thread sleep 1000

  testy ! "hi there!"
  Thread sleep 1000

  testy ! Nil
  Thread sleep 1000

  testy ! "sorry about that"
  Thread sleep 1000

  ourSystem stop testy
  Thread sleep 1000

  ourSystem.terminate()
  Thread sleep 3000

}
