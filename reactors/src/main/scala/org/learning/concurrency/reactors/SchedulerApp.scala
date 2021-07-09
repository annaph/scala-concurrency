package org.learning.concurrency.reactors

import io.reactors.{JvmScheduler, Proto, Reactor, ReactorPreempted, ReactorScheduled, ReactorSystem}

import scala.concurrent.duration._

object SchedulerApp extends App {

  val system = ReactorSystem default "reactor-system"
  val proto = Proto[LoggerReactor] withScheduler JvmScheduler.Key.globalExecutionContext
  val channel = system spawn proto

  sleep(1.second)

  channel ! "event 1"
  sleep(1.second)

  channel ! "event 2"
  sleep(1.second)

  channel ! "should not be printed!"

  sleep(3.seconds)

}

object SchedulerApp2 extends App {

  val schedulerName = "custom-timer"

  val system = ReactorSystem default "reactor-system"
  system.bundle.registerScheduler(name = schedulerName, new JvmScheduler.Timer(period = 1000))

  val proto = Proto[LoggerReactor] withScheduler schedulerName
  val channel = system spawn proto

  sleep(1.1.seconds)

  channel ! "event 1"
  sleep(100.milliseconds)

  channel ! "event 2"
  sleep(100.milliseconds)

  channel ! "event 3"
  sleep(100.milliseconds)

  channel ! "should be printed!"

  sleep(3.seconds)

}

class LoggerReactor extends Reactor[String] {
  private var count = 3

  sysEvents.onMatch {
    // Reactor scheduled
    case ReactorScheduled =>
      log(msg = "scheduled")

    // Reactor preempted
    case ReactorPreempted =>
      count = count - 1
      log(msg = s"preempted: count = $count")

      if (count == 0) {
        log(msg = "terminating")
        main.seal()
      }
  }

  main.events onEvent log

}
