package org.learning.concurrency.transactional.memory.exercises

import org.learning.concurrency.transactional.memory.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{InTxn, Ref, Txn, atomic}
import scala.util.{Failure, Random, Success, Try}

object AtomicWithRetryMaxApp extends App {

  val r = Ref(10)

  for (i <- 1 to 10) Future {
    Try {
      atomicWithRetryMax(3)(myBlock)
    } match {
      case Success(result) =>
        log(s"Transaction: $i - ok, result = $result")
      case Failure(ReachMaxNumberException(cntRetries)) =>
        log(s"Transaction: $i (retries = $cntRetries)")
      case Failure(e) =>
        e.printStackTrace()
        log(s"Error - $e")
    }
  }

  Thread sleep 12000

  def myBlock(inTxn: InTxn): Int = {
    val n = r.get(inTxn)
    val x = Random nextInt (10 * n)

    Thread sleep 10

    r.update(x)(inTxn)
    x
  }

  def atomicWithRetryMax[T](n: Int)(block: InTxn => T): T = {
    var cnt = 0
    atomic { implicit txn =>
      Txn.afterRollback(_ => cnt += 1)
      if (cnt == n) throw ReachMaxNumberException(cnt)
      block(txn)
    }
  }

  case class ReachMaxNumberException(cntRetries: Int) extends Exception

}
