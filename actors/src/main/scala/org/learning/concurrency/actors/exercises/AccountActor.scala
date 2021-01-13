package org.learning.concurrency.actors.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import org.learning.concurrency.actors.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object AccountActorApp extends App {

  implicit val timeout: Timeout = Timeout(3.seconds)

  val system = ActorSystem(name = "MyActorSystem")

  val account1 = system.actorOf {
    AccountActor.props(name = "Anna", initialAmount = 31d)
  }

  val account2 = system.actorOf {
    AccountActor.props(name = "Stacey")
  }

  for (i <- 0 until 3) {
    transaction(from = account1, to = account2, amount = 7d, i).onComplete {
      case Success(_) =>
        log(msg = "Transaction from Anna to Stacey succeeded.")
      case Failure(e) =>
        log(msg = "Transaction from Anna to Stacey failed!")
        e.printStackTrace()
    }
  }

  Thread sleep (1000 * 3)

  account1 ! AccountActor.Print
  account2 ! AccountActor.Print

  Thread sleep (1000 * 3)

  system.terminate()
  Thread sleep 3000

  def transaction(from: ActorRef, to: ActorRef, amount: Double, transactionId: Long): Future[Unit] = {
    val transactionActor = system.actorOf(TransactionActor.props, name = s"transaction-$transactionId")

    val result = transactionActor ? TransactionActor.Start(from, to, amount)
    result.collect {
      case TransactionActor.End => ()
    }
  }


}

class TransactionActor extends Actor {

  import TransactionActor._

  private val _log = Logging(context.system, this)

  private var _initiator: ActorRef = _

  private var _from: ActorRef = _

  private var _to: ActorRef = _

  private var _amount: Double = _

  override def receive: Receive = {
    case Start(from, to, amount) =>
      _initiator = sender()
      _from = from
      _to = to
      _amount = amount

      _from ! AccountActor.MinusMoney(amount)
      context become postTransferFrom
  }

  private def postTransferFrom: Receive = {
    case Ok =>
      _to ! AccountActor.PlusMoney(_amount)
      context become postTransferTo
    case Error =>
      _log error s"Transfer error (${_from})"
      context stop self
  }

  private def postTransferTo: Receive = {
    case Ok =>
      _log info "Transfer complete"
      _initiator ! End
      context stop self
  }

}

object TransactionActor {

  def props: Props =
    Props[TransactionActor]()

  case class Start(from: ActorRef, to: ActorRef, amount: Double)

  case object Ok

  case object Error

  case object End

}

class AccountActor(name: String, initialAmount: Double) extends Actor {

  import AccountActor._

  private val _log = Logging(context.system, this)

  private var _amount: Double = initialAmount

  override def receive: Receive = {
    case PlusMoney(amount) =>
      _amount += amount
      sender() ! TransactionActor.Ok
    case MinusMoney(amount) if _amount >= amount =>
      _amount -= amount
      sender() ! TransactionActor.Ok
    case MinusMoney(amount) =>
      _log error s"Insufficient funds. (${_amount} < $amount)"
      sender() ! TransactionActor.Error
    case Print =>
      _log info s"$name: ${+_amount}"
  }

}

object AccountActor {

  def props(name: String, initialAmount: Double = 0d): Props =
    Props(new AccountActor(name, initialAmount))

  case class PlusMoney(amount: Double)

  case class MinusMoney(amount: Double)

  case object Print

}
