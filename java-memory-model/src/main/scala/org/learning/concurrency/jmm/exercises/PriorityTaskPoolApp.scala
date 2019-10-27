package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.log

object PriorityTaskPoolApp extends App {

  val pool = new PriorityTaskPool

  for (_ <- 1 to 24) {
    val priority = (Math.random() * 1000).toInt

    pool.asynchronous(priority) {
      log(s"<- $priority")
    }
  }

  Thread sleep 7000

}

class PriorityTaskPool {

  import scala.collection.mutable

  type Task = () => Unit

  implicit val ordering: Ordering[(Int, Task)] = Ordering by (_._1)

  private val tasks = mutable.PriorityQueue.empty[(Int, Task)]

  Worker.start()

  def asynchronous(priority: Int)(task: => Unit): Unit = tasks.synchronized {
    tasks enqueue (priority -> (() => task))
    tasks.notify()
  }

  object Worker extends Thread {

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
