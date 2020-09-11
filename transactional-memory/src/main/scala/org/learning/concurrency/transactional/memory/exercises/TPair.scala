package org.learning.concurrency.transactional.memory.exercises

import org.learning.concurrency.transactional.memory.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{InTxn, Ref, Txn, atomic}

object TPairApp extends App {

  val pair = TPair("left value", "right value")

  for (_ <- 1 to 1001) Future(swapValues(pair))

  Thread sleep 7000

  atomic { implicit txn =>
    log(s"Result: left = '${pair.left}' | right = '${pair.right}'")
  }

  def swapValues[L, R](pair: TPair[String, String]): Unit = atomic { implicit txn =>
    pair.swap()

    val leftValue = pair.left
    val rightValue = pair.right

    Txn.afterCommit { _ =>
      assert(leftValue != rightValue)
    }
  }

}

class TPair[L, R](leftInit: L, rightInit: R) {

  private val _left = Ref[L](leftInit)

  private val _right = Ref[R](rightInit)

  def left(implicit txn: InTxn): L =
    _left.single()

  def left_(l: L)(implicit txn: InTxn): Unit =
    _left.single.transform(_ => l)

  def right(implicit txn: InTxn): R =
    _right.single()

  def right_(r: R)(implicit txn: InTxn): Unit =
    _right.single.transform(_ => r)

  def swap()(implicit e: L =:= R, txn: InTxn): Unit = {
    val oldRight = _right().asInstanceOf[L]

    _right update _left()
    _left update oldRight
  }

}

object TPair {

  def apply[L, R](left: L, right: R): TPair[L, R] =
    new TPair(left, right)

}
