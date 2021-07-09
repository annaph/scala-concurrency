package org.learning.concurrency.reactors

import io.reactors.Events.Emitter

object LuckyNumberApp extends App {

  val emitter = new Emitter[Int]
  var luckyNumber = 0

  emitter.onEvent(luckyNumber = _)

  emitter react 7
  assert(luckyNumber == 7, "luckyNumber should be 7")

  emitter react 8
  assert(luckyNumber == 8, "luckyNumber should be 8")

  log(msg = "Done!")

}
