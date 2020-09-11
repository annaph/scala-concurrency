package org.learning.concurrency.transactional.memory.exercises

import org.learning.concurrency.transactional.memory.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{InTxn, Ref, Txn, atomic}
import scala.util.Random

object AtomicRollbackCountApp extends App {

  val r = Ref(10)

  for (_ <- 1 to 10) Future {
    atomicRollbackCount(myBlock) match {
      case (result, count) =>
        log(s"Transaction = $result, retries = $count")
    }
  }

  Thread sleep 12000

  atomic { implicit txn =>
    log(s"r = ${r()}")
  }

  def myBlock(inTxn: InTxn): Int = {
    val n = r.get(inTxn)
    val x = Random nextInt (10 * n)

    Thread sleep 10

    r.update(x)(inTxn)
    x
  }

  def atomicRollbackCount[T](block: InTxn => T): (T, Int) = {
    var cnt = 0
    atomic { implicit txn =>
      Txn.afterRollback(_ => cnt += 1)
      block(txn) -> cnt
    }
  }

}
