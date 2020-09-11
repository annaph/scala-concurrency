package org.learning.concurrency.transactional.memory

import scala.annotation.tailrec
import scala.concurrent.stm.{Ref, atomic}

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
