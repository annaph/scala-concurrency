package org.learning.concurrency.transactional.memory.exercises

import java.util.concurrent.atomic.AtomicInteger

import org.learning.concurrency.transactional.memory.exercises.MVar.swap
import org.learning.concurrency.transactional.memory.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{InTxn, Ref, Txn, atomic, retry}

object MVarApp extends App {

  val mVar = MVar[Int]()

  // Producer
  for (i <- 1 to 12) Future {
    atomic { implicit txn =>
      mVar put i
    }
  }

  // Consumer
  val sum = new AtomicInteger(0)
  for (_ <- 1 to 12) Future {
    atomic { implicit txn =>
      val n = mVar.take()
      Txn.afterCommit(_ => sum addAndGet n)
    }
  }

  Thread sleep 12000

  val expectedSum = (1 to 12).sum
  assert(sum.get() == expectedSum, s"Error !!! ${sum.get()} != $expectedSum")

}

object MVarApp2 extends App {

  val mva = MVar[String]()
  val mvb = MVar[String]()

  atomic { implicit txn =>
    mva put "a"
    mvb put "b"
  }

  Thread sleep 3000

  for (_ <- 1 to 11) Future {
    atomic { implicit txn =>
      swap(mva, mvb)
    }
  }

  Thread sleep 12000

  atomic { implicit txn =>
    val a = mva.take()
    val b = mvb.take()

    Txn.afterCommit(_ => log(s"a = '$a', b = '$b'"))
  }

}

class MVar[T] {

  private val _value: Ref[Option[T]] = Ref(None)

  def put(x: T)(implicit txn: InTxn): Unit = _value() match {
    case None =>
      _value update Some(x)
    case Some(_) =>
      retry
  }

  def take()(implicit txn: InTxn): T = _value() match {
    case Some(value) =>
      _value update None
      value
    case None =>
      retry
  }

  def isEmpty(implicit txn: InTxn): Boolean =
    _value().isEmpty

}

object MVar {

  def apply[T](): MVar[T] =
    new MVar()

  def swap[T](mva: MVar[T], mvb: MVar[T])(implicit inTxn: InTxn): Unit = {
    if (mva.isEmpty || mvb.isEmpty) throw new Exception

    val a = mva.take()
    val b = mvb.take()

    mva put b
    mvb put a
  }

}
