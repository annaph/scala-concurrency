package org.learning.concurrency.blocks.exercises

import java.util.concurrent.atomic.AtomicReference

import org.learning.concurrency.blocks.{log, thread}

import scala.annotation.tailrec

sealed trait Element[+A]

case class Node[A: Ordering](head: A,
                             tail: AtomicReference[Element[A]] = new AtomicReference[Element[A]](Leaf))
  extends Element[A]

case object Leaf extends Element[Nothing]

class ConcurrentSortedList[A: Ordering] {

  private val sorted = new AtomicReference[Element[A]](Leaf)

  def add(a: A): Unit = {
    @tailrec
    def add(elementReference: AtomicReference[Element[A]], value: A): Unit = {
      val element = elementReference.get
      element match {
        case Node(head, _) if implicitly[Ordering[A]].compare(value, head) <= 0 =>
          val newNode = Node(value)
          newNode.tail.set(element)
          if (!elementReference.compareAndSet(element, newNode)) add(elementReference, value)
        case Node(_, tail) =>
          add(tail, value)
        case Leaf =>
          val node = Node(a)
          if (!elementReference.compareAndSet(element, node)) add(elementReference, value)
      }
    }

    add(sorted, a)
  }

  def iterator: Iterator[A] = new Iterator[A] {

    private var currElement = sorted.get

    override def hasNext: Boolean = currElement match {
      case Node(_, _) =>
        true
      case Leaf =>
        false
    }

    override def next(): A = currElement match {
      case Node(head, tail) =>
        currElement = tail.get
        head
      case Leaf =>
        throw new NoSuchElementException("next on empty iterator")
    }

  }

}

object ConcurrentSortedListApp extends App {

  val sortedList = new ConcurrentSortedList[Int]

  val producers = for (_ <- 1 to 100) yield {
    Thread sleep (math.random * 100).toInt
    thread {
      for (i <- 1 to 1000) {
        Thread sleep (math.random * 10).toInt
        sortedList add (math.random * 100 + i).toInt
      }
    }
  }

  producers.foreach(_.join())

  log(s"length = ${sortedList.iterator.length}")

  val length = sortedList.iterator.foldLeft[(Int, Int)](0, 0) { (acc, x) =>
    val length = acc._1
    val prev = acc._2

    log(s"$x")
    if (prev > x)
      throw new Exception(s"$prev > $x")

    (length + 1) -> x
  }._1

  if (length != sortedList.iterator.length)
    throw new Exception(s"$length != ${sortedList.iterator.length}")

  log(s"length = ${sortedList.iterator.length} ($length)")

}
