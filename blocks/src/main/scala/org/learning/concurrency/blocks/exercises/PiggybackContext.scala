package org.learning.concurrency.blocks.exercises

import org.learning.concurrency.blocks.log

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object PiggybackContextApp extends App {

  val ec = new PiggybackContext

  ec.execute(() => log("run"))

  ec.execute(() => {
    log("run (exception)")
    throw new Exception("test exception")
  })

}

class PiggybackContext extends ExecutionContext {

  override def execute(runnable: Runnable): Unit = Try(runnable.run()) match {
    case Success(_) =>
      log("result: OK")
    case Failure(e) =>
      reportFailure(e)
  }

  override def reportFailure(cause: Throwable): Unit =
    log(s"error: ${cause.getMessage}}")

}
