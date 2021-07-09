package org.learning.concurrency.reactors

import io.reactors.Events.Emitter

object EventStreamCompositionApp extends App {

  val emitter = new Emitter[Int]

  val sum = emitter
    .map(n => n * n)
    .scanPast(0)(_ + _)

  sum.onEvent(n => log(msg = s"$n"))

  for (i <- 0 until 5) emitter react i

  val numbers = new Emitter[Int]
  val even = numbers.filter(_ % 2 == 0)
  val odd = numbers.filter(_ % 2 != 0)
  val numbersAgain = even union odd

  numbersAgain.onEvent { n =>
    log(msg = s"Received: $n")
  }

  for (i <- 0 until 3) numbers react i

  log(msg = "Done!")

}
