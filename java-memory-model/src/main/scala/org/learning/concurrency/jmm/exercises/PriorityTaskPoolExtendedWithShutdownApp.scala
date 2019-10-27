package org.learning.concurrency.jmm.exercises

import org.learning.concurrency.jmm.log

object PriorityTaskPoolExtendedWithShutdownApp extends App {

  val pool = new PriorityTaskPoolExtendedWithShutdown(3, 353)

  for (_ <- 1 to 53) {
    val priority = (Math.random() * 1000).toInt

    pool.asynchronous(priority) {
      log(s"<- $priority")
    }
  }

  log("Shutdown all workers in the pool...")
  pool.shutdown()

  Thread sleep 7000

}

class PriorityTaskPoolExtendedWithShutdown(val n: Int, val important: Int) {

  import scala.collection.mutable

  type Task = () => Unit

  implicit val ordering: Ordering[(Int, Task)] = Ordering by (_._1)

  private val tasks = mutable.PriorityQueue.empty[(Int, Task)]

  private val workers: List[Worker] = List.range(0, n).map(i => new Worker(s"worker-$i"))

  private var terminated = false

  workers.foreach(_.start())

  def asynchronous(priority: Int)(task: => Unit): Unit = tasks.synchronized {
    tasks enqueue (priority -> (() => task))
    tasks.notify()
  }

  def shutdown(): Unit = tasks.synchronized {
    terminated = true
    tasks.notify()
  }

  class Worker(name: String) extends Thread {

    setName(name)

    setDaemon(true)

    override def run(): Unit =
      poll() match {
        case Some(task) =>
          task()
          run()
        case None =>
          ()
      }

    def poll(): Option[Task] = tasks.synchronized {
      while (tasks.isEmpty && !terminated) tasks.wait()

      (tasks.isEmpty, terminated) match {
        case (true, true) =>
          None
        case (_, true) =>
          log(s"${tasks.map(_._1).mkString(",")}")
          tasks.dequeue() match {
            case (priority, _) if priority <= important =>
              None
            case (_, task) =>
              Some(task)
          }
        case _ =>
          log(s"${tasks.map(_._1).mkString(",")}")
          Some(tasks.dequeue()._2)
      }
    }

  }

}
