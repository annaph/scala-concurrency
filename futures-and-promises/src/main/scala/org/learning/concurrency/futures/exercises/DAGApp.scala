package org.learning.concurrency.futures.exercises

import java.util.concurrent.atomic.AtomicLong

import org.learning.concurrency.log

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

object DAGApp extends App {

  val a = DAG("a")
  val b = DAG("b")
  val c = DAG("c")
  val d = DAG("d")
  val e = DAG("e")

  a addEdge b
  b addEdge c
  b addEdge d
  c addEdge e
  d addEdge e

  log("DAG graph\n:")
  log(s"${a.toString}")

  val task: (String, Seq[String]) => String = (value, inputs) => {
    s"$value -> ${inputs.mkString("(", " | ", ")")}"
  }

  a.fold(task).onComplete {
    case Success(result) =>
      log(s"Fold result: $result")
    case Failure(e) =>
      log(s"Error: ${e.getMessage}")
  }

  val simpleDAG = DAG("a")
  simpleDAG.fold(task).foreach { result =>
    log(s"Simple DAG result: $result")
  }

  Thread sleep 3000

}

class DAG[T](val value: T) {

  private val _edges = mutable.Set.empty[DAG[T]]

  def edges: Seq[DAG[T]] =
    _edges.toSeq

  def addEdge(other: DAG[T]): Unit =
    _edges += other

  def fold[S](f: (T, Seq[S]) => S): Future[S] =
    DAG.fold(this, f)

  override def toString: String = {
    val thisStr = s"$value => " + (_edges.map(_.value) mkString " & ")
    val othersStr = _edges.toList.map(edge => edge.toString)

    (thisStr +: othersStr) mkString "\n"
  }

}

object DAG {

  def apply[T](value: T): DAG[T] =
    new DAG(value)

  def fold[T, S](dag: DAG[T], f: (T, Seq[S]) => S): Future[S] = {
    val dagExecution = DAGExecution(dag, f)

    log("DAGExecution graph:\n")
    log(s"${dagExecution.toString}")
    log(s"Leaf: ${dagExecution.leaf.toString}")

    DAGExecution run dagExecution
  }

}

class DAGExecution[T, S](val value: T,
                         val promise: Promise[S],
                         val task: (T, Seq[S]) => S,
                         val children: mutable.Set[DAGExecution[T, S]] = mutable.Set.empty[DAGExecution[T, S]]) {

  private val _id: Long = DAGExecution.generateId

  def leaf: DAGExecution[T, S] = {
    @tailrec
    def go(dExec: DAGExecution[T, S]): DAGExecution[T, S] = dExec.children.size match {
      case 0 => dExec
      case _ => go(dExec.children.head)
    }

    go(this)
  }

  def runAsync: Future[Unit] = Future {
    val childrenFutures = children.map(_.promise.future).toSeq

    childrenFutures match {
      case Seq() =>
        promise tryComplete Try(task(value, Seq.empty[S]))
      case _ =>
        Future.sequence(childrenFutures)
          .map(task(value, _))
          .onComplete {
            case Success(result) =>
              promise success result
            case Failure(e) =>
              promise failure e
          }
    }
  }

  override def equals(that: Any): Boolean = that match {
    case obj: DAGExecution[T, S] =>
      (obj canEqual this) && (this._id == obj._id)
    case _ =>
      false
  }

  def canEqual(that: Any): Boolean = that.isInstanceOf[DAGExecution[S, T]]

  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + _id.hashCode();

    result
  }

  override def toString: String = {
    val thisStr = s"(${_id},$value) => " + (children.map(_.value) mkString " & ")
    val othersStr = children.toList.map(children => children.toString)

    (thisStr +: othersStr) mkString "\n"
  }

}

object DAGExecution {

  private val _ID_COUNT = new AtomicLong(0)

  def apply[T, S](dag: DAG[T], task: (T, Seq[S]) => S): DAGExecution[T, S] = {
    val map = mutable.Map.empty[DAG[T], DAGExecution[T, S]]

    def go(d: DAG[T], dExec: DAGExecution[T, S]): Unit = {
      val pairChildren = d.edges.map(edge => edge -> getOrCreateDagExecution(edge, task, map))
      dExec.children ++= pairChildren.map(_._2)

      pairChildren.foreach(child => go(child._1, child._2))
    }

    val dagExecution = getOrCreateDagExecution(dag, task, map)
    go(dag, dagExecution)

    dagExecution
  }

  private def getOrCreateDagExecution[T, S](d: DAG[T],
                                            task: (T, Seq[S]) => S,
                                            map: mutable.Map[DAG[T], DAGExecution[T, S]]): DAGExecution[T, S] =
    map.get(d) match {
      case Some(dExec) =>
        dExec
      case None =>
        val dExec = new DAGExecution[T, S](value = d.value, promise = Promise[S], task = task)
        map.put(d, dExec)
        dExec
    }

  def run[T, S](dagExecution: DAGExecution[T, S]): Future[S] = {
    def go(dExec: DAGExecution[T, S]): Unit = {
      dExec.runAsync
      dExec.children.foreach(go)
    }

    go(dagExecution)
    dagExecution.promise.future
  }

  private def generateId: Long =
    _ID_COUNT.incrementAndGet()

}
