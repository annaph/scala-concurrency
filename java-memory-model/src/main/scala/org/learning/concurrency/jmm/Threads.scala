package org.learning.concurrency.jmm

object ThreadsMain extends App {

  val name = Thread.currentThread.getName
  println(s"I am the thread $name")

}

object ThreadsCreation extends App {

  class MyThread extends Thread {
    override def run(): Unit =
      println("New thread running.")
  }

  val t = new MyThread

  t.start()
  t.join()

  println("New thread joined.")

}

object ThreadsSleep extends App {

  val t = thread {
    Thread sleep 1000
    println("New thread running.")

    Thread sleep 1000
    println("Still running.")

    Thread sleep 1000
    println("Completed.")
  }

  t.join()
  println("New thread joined.")

}

object ThreadsNondeterministic extends App {

  val t = thread {
    log("New thread running.")
  }

  log("...")
  log("...")
  log("...")

  t.join()
  log("New thread joined.")

}

object ThreadsCommunicate extends App {

  var result: String = _

  val t = thread {
    result = System.lineSeparator +
      List.fill(5)(s"Title${System.lineSeparator}=").mkString(System.lineSeparator)
  }

  t.join()

  log(result)

}

object ThreadsUnprotectedUid extends App {

  var uidCount = 0L

  def uniqueId(): Long = {
    val freshUid = uidCount + 1L
    uidCount = freshUid

    freshUid
  }

  def printUniqueUids(n: Int): Unit = {
    val uids =
      for {
        _ <- 0 until n
      } yield uniqueId()

    log(s"Generated uids: $uids")
  }

  val t = thread(printUniqueUids(5))

  printUniqueUids(5)

  t.join()

}
