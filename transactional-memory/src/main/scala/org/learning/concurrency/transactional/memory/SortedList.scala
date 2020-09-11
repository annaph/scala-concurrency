package org.learning.concurrency.transactional.memory

import scala.annotation.tailrec
import scala.concurrent.stm.{Ref, TxnLocal, atomic, retry}

case class SortedList(head: Ref[Option[Node]] = Ref(None)) {

  private val _log = TxnLocal("")

  def headWait: Int = atomic { implicit txn =>
    head() match {
      case Some(node) =>
        node.elem
      case None =>
        retry
    }
  }

  def headWaitBad: Int = atomic { implicit txn =>
    while (head().isEmpty) {}
    head().get.elem
  }

  def insert(x: Int): this.type = atomic { implicit txn =>
    @tailrec
    def go(ref: Ref[Option[Node]]): Unit = ref() match {
      case Some(node) if x < node.elem =>
        ref update Some(Node(x, Ref(Some(node))))
      case Some(node) =>
        go(node.next)
      case None =>
        ref update Some(Node(x))
    }

    go(head)
    this
  }

  def pop(n: Int): this.type = atomic { implicit txn =>
    @tailrec
    def go(i: Int, ref: Ref[Option[Node]]): Unit = (i, ref()) match {
      case (0, x) =>
        head update x
      case (_, Some(node)) =>
        go(i - 1, node.next)
      case (_, None) =>
        throw new NullPointerException
    }

    go(n, head)
    this
  }

  def clearWithLog(): String = atomic { implicit txn =>
    clear()
    _log()
  }

  def clear(): this.type = atomic { implicit txn =>
    @tailrec
    def go(ref: Ref[Option[Node]]): Unit = ref() match {
      case Some(node) =>
        head update node.next()
        _log update s"${_log()}\nremoved '${node.elem}'"
        go(node.next)
      case None =>
        ()
    }

    go(head)
    this
  }

  override def toString: String = atomic { implicit txn =>
    head() match {
      case Some(headNode) =>
        headNode.toString
      case None =>
        "<empty>"
    }
  }

}
