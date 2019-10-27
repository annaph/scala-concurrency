package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.log

object PriorityTaskPoolExtendedApp extends App {

  val pool = new PriorityTaskPoolExtended(3)

  for (_ <- 1 to 24) {
    val priority = (Math.random() * 1000).toInt

    pool.asynchronous(priority) {
      log(s"<- $priority")
    }
  }

  Thread sleep 7000

}

class PriorityTaskPoolExtended(val n: Int) {

  import scala.collection.mutable

  type Task = () => Unit

  implicit val ordering: Ordering[(Int, Task)] = Ordering by (_._1)

  private val tasks = mutable.PriorityQueue.empty[(Int, Task)]

  private val workers: List[Worker] = List.range(0, n).map(i => new Worker(s"worker-$i"))

  workers.foreach(_.start())

  def asynchronous(priority: Int)(task: => Unit): Unit = tasks.synchronized {
    tasks enqueue (priority -> (() => task))
    tasks.notify()
  }

  class Worker(name: String) extends Thread {

    setName(name)

    setDaemon(true)

    override def run(): Unit =
      while (true) {
        val task = poll()
        task()
      }

    def poll(): Task = tasks.synchronized {
      while (tasks.isEmpty) tasks.wait()

      log(s"${tasks.map(_._1).mkString(",")}")
      tasks.dequeue()._2
    }

  }

}
