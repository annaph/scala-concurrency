package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ScatterGatherApp extends App {

  type Task[T] = () => T

  val task1 = () => "Anna"
  val task2 = () => "Nicole"
  val task3 = () => "Stacey"

  scatterGather(Seq(task1, task2, task3)).foreach { seq =>
    log(seq mkString ", ")
  }

  Thread sleep 3000

  def scatterGather[T](tasks: Seq[Task[T]]): Future[Seq[T]] = {
    val futures = tasks.map(task => Future(task()))
    Future.sequence(futures)
  }

}
