package org.learning.concurrency.transactional.memory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.stm._
import scala.concurrent.{Future, blocking}
import scala.util.{Failure, Success}

object RetryHeadWaitBad extends App {

  val myList = SortedList()

  Future {
    log("Getting the first element...")

    val headElem = myList.headWaitBad
    log(s"The first element is $headElem")
  }

  Thread sleep 1000
  log("Inserting the first element...")
  Future(myList insert 1)

  Thread sleep 1000

}

object RetryHeadWait extends App {

  val myList = SortedList()

  Future {
    blocking {
      val headElem = myList.headWait
      log(s"The first element is $headElem")
    }
  }

  Thread sleep 3000
  Future(myList insert 1)

  Thread sleep 3000

}

object RetryChaining extends App {

  val queue1 = SortedList()
  val queue2 = SortedList()

  // Consumer
  Future {
    blocking {
      atomic { implicit txn =>
        log("probing queue1...")
        val headElem = queue1.headWait
        log(s"got: $headElem")
      } orAtomic { implicit txn =>
        log(s"probing queue2...")
        val headElem = queue2.headWait
        log(s"got: $headElem")
      }
    }
  }

  // Producers
  Thread sleep 50
  Future(queue2 insert 2)

  Thread sleep 20
  Future(queue1 insert 1)

  Thread sleep 3000

}

object RetryTimeouts extends App {

  val message = Ref("")

  // Consumer
  Future {
    blocking {
      atomic.withRetryTimeout(1000) { implicit txn =>
        val msg = message()
        if (msg.nonEmpty) log(s"got a message - $msg") else retry
      }
    }
  }.onComplete {
    case Success(_) => ()
    case Failure(e) => log(s"got an error - ${e.getCause}")
  }

  // Producer
  Thread sleep 1500
  message.single() = "Howdy"

  Thread sleep 3000

}

object RetryFor extends App {

  val message = Ref("")

  // Consumer
  Future {
    blocking {
      atomic { implicit txn =>
        val msg = message()
        if (msg.isEmpty) {
          retryFor(1000)
          log(s"no message - '$msg'")
        } else log(s"got a message - '$msg'")
      }
    }
  }

  // Producer
  Thread sleep 1500
  message.single() = "Howdy!"

  Thread sleep 3000

}
