package org.learning.concurrency.reactors

import io.reactors.{Proto, Reactor, ReactorDied, ReactorPreempted, ReactorScheduled, ReactorStarted, ReactorSystem, ReactorTerminated}

import scala.concurrent.duration._

object LifecycleReactorApp extends App {

  val system = ReactorSystem default "reactor-system"
  val proto = Proto[LifecycleReactor]
  val channel = system spawn proto

  sleep(1.second)

  channel ! "event"

  sleep(3.seconds)

}

class LifecycleReactor extends Reactor[String] {
  private var first = true

  sysEvents.onMatch {
    case ReactorStarted =>
      log(msg = "started")

    case ReactorScheduled =>
      log(msg = "scheduled")

    case ReactorPreempted =>
      log(msg = "preempted")
      if (first) first = false else throw new Exception("My exception")

    case ReactorDied(e) =>
      log(msg = s"died: ${e.getMessage}")

    case ReactorTerminated =>
      log(msg = "terminated")
  }

}
