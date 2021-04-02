package org.learning.concurrency.debugging

object Deadlock extends App {

  val a = new Account(money = 1000)
  val b = new Account(money = 2000)

  val t1 = thread {
    for (_ <- 0 until 100) send(a, b, 1)
  }

  val t2 = thread {
    for (_ <- 0 until 100) send(b, a, 1)
  }

  t1.join()
  t2.join()

  def send(a: Account, b: Account, n: Int): Unit = a.synchronized {
    b.synchronized {
      a.money -= n
      b.money += n
    }
  }

  class Account(var money: Int)

}
