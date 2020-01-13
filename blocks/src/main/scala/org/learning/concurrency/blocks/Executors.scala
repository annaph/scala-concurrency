package org.learning.concurrency.blocks

import java.util.concurrent.{ForkJoinPool, TimeUnit}

import scala.concurrent.ExecutionContext

object ExecutorsCreate extends App {

  val executor = new ForkJoinPool

  executor.execute { () =>
    log("This task is run asynchronously.")
  }

  executor.shutdown()
  executor awaitTermination(3, TimeUnit.SECONDS)

}

object ExecutionContextGlobal extends App {

  val exCtx = ExecutionContext.global

  exCtx.execute { () =>
    log("Running on the execution context.")
  }

  Thread sleep 3000

}

object ExecutionContextCreate extends App {

  val exCtx = ExecutionContext fromExecutorService new ForkJoinPool(2)

  exCtx.execute { () =>
    log("Running on the execution context again.")
  }

  Thread sleep 3000

}

object ExecutionContextSleep extends App {

  for (i <- 1 to 16) execute {
    Thread sleep 2000
    log(s"Task $i completed.")
  }

  Thread sleep 12000

}
