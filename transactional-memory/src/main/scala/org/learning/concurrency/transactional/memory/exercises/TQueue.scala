package org.learning.concurrency.transactional.memory.exercises

import org.learning.concurrency.transactional.memory.log

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{InTxn, Ref, Txn, atomic, retry}

object TQueueApp extends App {

  val queue = TQueue[Int]()

  // Consumer
  for (_ <- 1 to 7) Future {
    atomic { implicit txn =>
      val n = queue.dequeue()
      Txn.afterCommit(_ => log(s"dequeue: $n"))
    }
  }

  Thread sleep 50

  // Producer
  for (i <- 1 to 7) Future {
    atomic { implicit txn =>
      queue enqueue i
      Txn.afterCommit(_ => log(s"enqueue: $i"))
    }
  }

  Thread sleep 7000

}

class TQueue[T] {

  private val _queue = Ref(Queue.empty[T])

  def enqueue(x: T)(implicit txn: InTxn): Unit = {
    val q = _queue()
    _queue update (q enqueue x)
  }

  def dequeue()(implicit txn: InTxn): T =
    _queue().dequeueOption match {
      case Some((x, q)) =>
        _queue update q
        x
      case None =>
        retry
    }

}

object TQueue {

  def apply[T](): TQueue[T] =
    new TQueue()

}