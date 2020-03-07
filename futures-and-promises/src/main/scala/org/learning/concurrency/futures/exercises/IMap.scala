package org.learning.concurrency.futures.exercises

import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

object IMapApp extends App {

  val map = new IMap[Int, String]

  val consumers = for (_ <- 0 until 7) yield {
    map(1)
  }

  consumers.foreach { f =>
    f.onComplete {
      case Success(value) =>
        log(s"key '1' value = $value")
      case Failure(e) =>
        log(s"Error !!! ${e.toString}")
    }
  }

  val producers = for (_ <- 0 until 101) yield {
    Future(map put(1, "a"))
  }

  producers.foreach { f =>
    f.onComplete {
      case Success(_) =>
        log(s"Adding value 'a' for key '1'")
      case Failure(_) =>
    }
  }

  consumers.foreach(Await.ready(_, Duration.Inf))
  producers.foreach(Await.ready(_, Duration.Inf))

}

class IMap[K, V] {

  import scala.collection.concurrent

  private val _map = concurrent.TrieMap.empty[K, Promise[V]]

  def apply(key: K): Future[V] = _map.get(key) match {
    case Some(promise) =>
      promise.future
    case None =>
      getOrPutPromise(key).future
  }

  def put(key: K, value: V): Unit =
    _map.putIfAbsent(key, createCompletedPromise(value)) match {
      case Some(existingPromise) =>
        if (!existingPromise.trySuccess(value))
          throw new IllegalStateException("A specific key can only be assigned once")
      case None =>
    }

  private def getOrPutPromise(key: K): Promise[V] = {
    val promise = Promise[V]

    _map.putIfAbsent(key, promise) match {
      case Some(existingPromise) =>
        existingPromise
      case None =>
        promise
    }
  }

  private def createCompletedPromise(value: V): Promise[V] = {
    val promise = Promise[V]
    promise success value

    promise
  }

}
