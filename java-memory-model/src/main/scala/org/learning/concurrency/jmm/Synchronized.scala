package org.learning.concurrency.jmm

import scala.annotation.tailrec

object SynchronizedProtectedUid extends App {

  var uidCount = 0L

  def uniqueUid(): Long = this.synchronized {
    val freshUid = uidCount + 1L
    uidCount = freshUid

    freshUid
  }

  def printUniqueUids(n: Int): Unit = {
    val uids =
      for {
        _ <- 0 until n
      } yield uniqueUid()

    log(s"Generated uids: $uids")
  }

  val t = thread(printUniqueUids(5))

  printUniqueUids(5)

  t.join()

}

object ThreadSharedStateAccessReordering extends App {

  for (_ <- 0 until 10000) {
    var a = false
    var b = false
    var x = -1
    var y = -1

    val t1 = thread {
      Thread sleep 1

      this.synchronized {
        a = true
      }

      y = if (b) 0 else 1
    }

    val t2 = thread {
      Thread sleep 1

      this.synchronized {
        b = true
      }

      x = if (a) 0 else 1
    }

    t1.join()
    t2.join()

    assert(!(x == 1 && y == 1), s"x=$x, y=$y")

  }

}

object SynchronizedNesting extends App {

  import scala.collection.mutable

  private val transfers = mutable.ArrayBuffer.empty[String]

  class Account(val name: String, var money: Int)

  def add(account: Account, n: Int): Unit = account.synchronized {
    account.money += n
    if (n > 10) logTransfer(account.name, n)
  }

  def logTransfer(name: String, n: Int): Unit = transfers.synchronized {
    transfers += s"Transfer to account '$name' = $n"
  }

  val anna = new Account("Anna", 100)
  val john = new Account("John", 200)

  val t1 = thread {
    add(anna, 5)
  }

  val t2 = thread {
    add(john, 50)
  }

  val t3 = thread {
    add(anna, 70)
  }

  t1.join()
  t2.join()
  t3.join()

  log(s"--- transfers ---\n$transfers")

}

object SynchronizedDeadlock extends App {

  import SynchronizedNesting.Account

  def send(a: Account, b: Account, n: Int): Unit = a.synchronized {
    b.synchronized {
      a.money -= n
      b.money += n
    }
  }

  val anna = new Account("Anna", 1000)
  val richard = new Account("Richard", 2000)

  val t1 = thread {
    for (_ <- 0 until 100) send(anna, richard, 1)
  }

  val t2 = thread {
    for (_ <- 0 until 100) send(richard, anna, 1)
  }

  t1.join()
  t2.join()

  log(s"anna = ${anna.money}, richard = ${richard.money}")

}

object SynchronizedNoDeadlock extends App {

  import SynchronizedProtectedUid.uniqueUid

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

  val anna = new Account("Anna", 1000)
  val richard = new Account("Richard", 2000)

  val t1 = thread {
    for (_ <- 0 until 100) send(anna, richard, 1)
  }

  val t2 = thread {
    for (_ <- 0 until 100) send(richard, anna, 1)
  }

  t1.join()
  t2.join()

  log(s"anna = ${anna.money}, richard = ${richard.money}")

}

object SynchronizedBadPool extends App {

  import scala.collection.mutable

  type Task = () => Unit

  private val tasks = mutable.Queue.empty[Task]

  val worker: Thread = new Thread {

    def poll(): Option[Task] = tasks.synchronized {
      tasks match {
        case _ if tasks.nonEmpty =>
          Some(tasks.dequeue())
        case _ =>
          None
      }
    }

    override def run(): Unit =
      while (true) poll() match {
        case Some(task) =>
          task()
        case None =>
          ()
      }

  }

  worker setDaemon true
  worker.start()

  def asynchronous(body: => Unit): Unit = tasks.synchronized {
    tasks.enqueue(() => body)
  }

  asynchronous(log("Hello"))
  asynchronous(log(" World!"))

  Thread sleep 7000

}

object SynchronizedGuardedBlocks extends App {

  val lock = new AnyRef

  var message: Option[String] = None

  val greeter = thread {
    lock.synchronized {
      while (message.isEmpty) lock.wait()
      log(message.get)
    }
  }

  lock.synchronized {
    message = Some("Hello!")
    lock.notify()
  }

  greeter.join()

}

object SynchronizedPool extends App {

  import scala.collection.mutable

  type Task = () => Unit

  private val tasks = mutable.Queue.empty[Task]

  object Worker extends Thread {

    setDaemon(true)

    override def run(): Unit =
      while (true) {
        val task = poll()
        task()
      }

    def poll(): Task = tasks.synchronized {
      while (tasks.isEmpty) tasks.wait()
      tasks.dequeue()
    }

  }

  def asynchronous(body: => Unit): Unit = tasks.synchronized {
    tasks.enqueue(() => body)
    tasks.notify()
  }

  Worker.start()

  asynchronous(log("Hello"))
  asynchronous(log(" World!"))

  Thread sleep 7000

}

object SynchronizedGracefulShutdown extends App {

  import scala.collection.mutable

  type Task = () => Unit

  private val tasks = mutable.Queue.empty[Task]

  object Worker extends Thread {

    private var terminated = false

    def poll(): Option[Task] = tasks.synchronized {
      while (tasks.isEmpty && !terminated) tasks.wait()
      if (terminated) None else Some(tasks.dequeue())
    }

    @tailrec
    override def run(): Unit =
      poll() match {
        case Some(task) =>
          task()
          run()
        case None =>
          ()
      }

    def shutdown(): Unit = tasks.synchronized {
      terminated = true
      tasks.notify()
    }

  }

  def asynchronous(body: => Unit): Unit = tasks.synchronized {
    tasks.enqueue(() => body)
    tasks.notify()
  }

  Worker.start()

  asynchronous(log("Hello"))
  asynchronous(log(" World!"))

  Thread sleep 7000

  Worker.shutdown()

}
