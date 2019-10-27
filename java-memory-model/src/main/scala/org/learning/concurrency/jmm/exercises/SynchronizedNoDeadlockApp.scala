package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.SynchronizedProtectedUid.uniqueUid
import org.learning.concurrency.jmm.log

object SynchronizedNoDeadlockApp extends App {

  class Account(val name: String, var money: Int) {
    val uid: Long = uniqueUid()
  }

  def send(a: Account, b: Account, n: Int): Unit = {
    def adjust(): Unit = {
      a.money -= n
      b.money += n
    }

    if (a.uid < b.uid) a.synchronized(b.synchronized(adjust()))
    else b.synchronized(a.synchronized(adjust()))
  }

  def sendAll(accounts: Set[Account], target: Account): Unit = {
    def adjust(): Unit = {
      accounts.foreach { acc =>
        target.money += acc.money
        acc.money = 0
      }
    }

    def sendAllWithSynchronize(xs: List[Account]): Unit = xs match {
      case x :: xss =>
        x synchronized sendAllWithSynchronize(xss)
      case Nil =>
        adjust()
    }

    val allAccounts = target :: accounts.toList
    sendAllWithSynchronize(allAccounts sortBy (_.uid))
  }

  val anna = new Account("Anna", 0)

  val richard = new Account("Richard", 2000)
  val nicole = new Account("Nicole", 500)
  val stacey = new Account("Stacey", 400)

  val accounts = Set(richard, nicole, stacey)

  sendAll(accounts, anna)

  accounts.foreach { acc =>
    log(s"${acc.name}, money = ${acc.money}")
  }

  log(s"${anna.name}, money = ${anna.money}")

}
