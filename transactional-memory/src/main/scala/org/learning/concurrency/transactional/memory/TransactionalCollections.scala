package org.learning.concurrency.transactional.memory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{TArray, TMap, atomic}

object TransactionLocals extends App {

  val sortedList = SortedList()

  sortedList.insert(4).insert(9).insert(1).insert(16)
  log(s"sorted list - $sortedList")

  sortedList.clear()
  log(s"sorted list after clear - $sortedList")

  val sortedList2 = SortedList()
  sortedList2.insert(14).insert(22)
  log(s"sorted list2 - $sortedList2")

  Thread sleep 1000

  val f = Future(sortedList2.clearWithLog())
  val g = Future(sortedList2.clearWithLog())

  for {
    log1 <- f
    log2 <- g
  } log(s"Log for f: $log1\nLog for g: $log2")

  Thread sleep 3000

}

object TransactionalArray extends App {

  val pages = Seq.fill(5)("Scala 2.12 is out")
  val website = TArray(pages)

  val f = Future(replace("2.12", "2.13"))
  val g = Future(replace("out", "released"))

  for {
    _ <- f
    _ <- g
  } log(s"Document\n$asString")

  Thread sleep 3000

  def replace(oldStr: String, newStr: String): Unit = atomic { implicit txn =>
    for (i <- 0 until website.length) website(i) = website(i).replace(oldStr, newStr)
  }

  def asString: String = atomic { implicit txn =>
    val result = for {
      i <- 0 until website.length
    } yield s"Page $i\n=======\n${website(i)}"

    result.mkString("", "\n\n", "\n")
  }

}

object TransactionalMap extends App {

  val alphabet = TMap("a" -> 1, "B" -> 2, "C" -> 3)

  Future {
    atomic { implicit txn =>
      alphabet.put("A", 1)
      alphabet remove "a"
    }
  }

  Thread sleep 14

  Future {
    val snapshot = alphabet.single.snapshot
    log(s"atomic snapshot: $snapshot")
  }

  Thread sleep 3000

}
