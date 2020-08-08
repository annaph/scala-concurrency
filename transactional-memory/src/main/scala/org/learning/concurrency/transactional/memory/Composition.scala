package org.learning.concurrency.transactional.memory

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{Ref, Txn, atomic}

case class Node(elem: Int, next: Ref[Option[Node]] = Ref(None)) {

  def append(node: Node): Unit = atomic { implicit txn =>
    @tailrec
    def go(n: Node): Unit = n.next() match {
      case None =>
        n.next update Some(node)
      case Some(x) =>
        go(x)
    }

    go(this)
  }

  def appendIfEnd(node: Node): Unit = next.single.transform {
    case x@Some(_) =>
      x
    case None =>
      Some(node)
  }

  def nextNode: Option[Node] =
    next.single()

  def lastNode: Node = atomic { implicit txn =>
    @tailrec
    def go(n: Node): Node = n.next() match {
      case Some(x) =>
        go(x)
      case None =>
        n
    }

    go(this)
  }

  override def toString: String = atomic { implicit txn =>
    @tailrec
    def go(n: Node, acc: StringBuilder): StringBuilder = n.next() match {
      case None =>
        acc ++= n.elem.toString
      case Some(x) =>
        go(x, acc ++= s"${n.elem.toString}|")
    }

    go(this, new StringBuilder).toString
  }

}

object CompositionSideEffects extends App {

  val myValue = Ref(0)

  def inc(): Unit = atomic { implicit txn =>
    log(s"Incrementing ${myValue()}")

    val newMyValue = myValue() + 1
    myValue update newMyValue
  }

  Future(inc())
  Future(inc())

  Thread sleep 3000

}

object CompositionCorrectSideEffect extends App {

  val myValue = Ref(0)

  def inc(): Unit = atomic { implicit txn =>
    val valueAtStart = myValue()

    Txn.afterCommit { _ =>
      log(s"Incrementing $valueAtStart")
    }

    val newValue = myValue() + 1
    myValue update newValue
  }

  Future(inc())
  Future(inc())

  Thread sleep 3000

}

object CompositionLoggingRollback extends App {

  val myValue = Ref(0)

  def inc(): Unit = atomic { implicit txn =>
    val valueAtStart = myValue()

    Txn.afterCommit { _ =>
      log(s"Incrementing $valueAtStart")
    }

    Txn.afterRollback { _ =>
      log("rollin' back")
    }

    val newValue = myValue() + 1
    myValue update newValue
  }

  Future(inc())
  Future(inc())

  Thread sleep 3000

}

object CompositionMutations extends App {

  val node1 = Node(1)
  val node2 = Node(2, Ref(Some(node1)))
  val node3 = Node(3, Ref(Some(node2)))

  log(s"Node: $node3")

}

object CompositionList extends App {

  val node5 = Node(5)
  val node4 = Node(4, Ref(Some(node5)))
  val node1 = Node(1, Ref(Some(node4)))

  log(s"Node: $node1")

  Thread sleep 1000

  val f = Future(node1 append Node(2))
  val g = Future(node1 append Node(3))

  for {
    _ <- f
    _ <- g
  } {
    log(s"Node after append: $node1")
    log(s"Last node elem is: ${node1.lastNode.elem}")
  }

  Thread sleep 3000

  val node6 = Node(6)
  node6 appendIfEnd Node(7)
  log(s"Node6 after appendIfEnd: $node6")

  node1 appendIfEnd node6
  log(s"Node1 after appendIfEnd: $node1")

}
