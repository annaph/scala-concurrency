package org.learning.concurrency.reactors

import io.reactors.Events.Emitter
import io.reactors.Observer

object EventStreamLifecycleApp extends App {

  val emitter = new Emitter[Int]

  var seen = List.empty[Int]
  var errors = List.empty[String]
  var done = 0

  emitter.onReaction(new Observer[Int] {

    override def react(n: Int, hint: Any): Unit =
      seen = n :: seen

    override def except(e: Throwable): Unit =
      errors = e.getMessage :: errors

    override def unreact(): Unit =
      done = 1

  })

  emitter react 1
  emitter react 2
  emitter except new Exception("^_^")
  emitter react 3

  assert(seen == List(3, 2, 1), message = "seen should contain: (3, 2, 1)")
  assert(errors == List("^_^"), message = "error should contain: (\"^_^\")")
  assert(done == 0, message = "done should be 0")

  emitter.unreact()

  assert(done == 1, message = "done should be 1")

  emitter react 4
  emitter except new Exception("o_O")

  assert(seen == List(3, 2, 1), message = "seen should contain: (3, 2, 1)")
  assert(errors == List("^_^"), message = "error should contain: (\"^_^\")")
  assert(done == 1, message = "done should be 1")

  log(msg = "Done!")

}
