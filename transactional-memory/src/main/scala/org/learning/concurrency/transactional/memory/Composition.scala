package org.learning.concurrency.transactional.memory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{Ref, Txn, atomic}
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.ControlThrowable
import scala.util.{Failure, Success, Try}

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

object CompositionSortedList extends App {

  val sortedList = SortedList()

  val f = Future {
    sortedList insert 1
    sortedList insert 4
  }

  val g = Future {
    sortedList insert 5
    sortedList insert 2
    sortedList insert 3
  }

  for {
    _ <- f
    _ <- g
  } log(s"sorted list - $sortedList")

  Thread sleep 3000

}

object CompositionExceptions extends App {

  val sortedList = SortedList()
  sortedList.insert(4).insert(9).insert(1).insert(16)

  log(s"sorted list - $sortedList")

  Future(sortedList pop 2).foreach(_ => log(s"removed 2 elements - $sortedList"))
  Thread sleep 1000

  Future(sortedList pop 3).failed.foreach(e => log(s"oops $e - $sortedList"))
  Thread sleep 3000

  Future {
    atomic { implicit txn =>
      sortedList pop 1
      throw new Exception
    }
  }.failed.foreach(e => log(s"oops again $e - $sortedList"))
  Thread sleep 1000

  Future {
    breakable {
      atomic { implicit txn =>
        for (i <- 1 to 3) {
          sortedList pop i
          break()
        }
      }
    }
  }
  Thread sleep 1000
  log(s"after removing - $sortedList")

  Future {
    breakable {
      atomic.withControlFlowRecognizer(controlThrowable) { implicit txn =>
        for (i <- 1 to 3) {
          sortedList pop i
          break()
        }
      }
    }
  }
  Thread sleep 1000
  log(s"after removing again - $sortedList")

  private def controlThrowable: PartialFunction[Throwable, Boolean] = {
    case _: ControlThrowable =>
      false
  }

}

object CompositionCatchingExceptions extends App {

  val sortedList = SortedList()
  sortedList.insert(4).insert(9).insert(1).insert(16)

  log(s"sortedList before - $sortedList")

  atomic { implicit txn =>
    sortedList pop 2
    log(s"sortedList - $sortedList")

    Try(sortedList pop 3) match {
      case Success(_) =>
      case Failure(e) =>
        log(s"Houston... $e")
    }

    sortedList pop 1
  }

  log(s"result - $sortedList")

}
