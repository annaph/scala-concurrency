package org.learning.concurrency.reactors

import io.reactors.{Channel, Proto, Reactor, ReactorSystem}

import scala.concurrent.duration._

object ReactorExampleApp extends App {

  // Create Reactor System
  val system = new ReactorSystem(name = "test-system")

  // Create Reactor template
  val proto: Proto[Reactor[String]] = Reactor[String] { self =>
    self.main.events.onEvent { str =>
      log(msg = s"Received: $str")
    }
  }

  // Create Reactor
  val channel: Channel[String] = system spawn proto

  // Send events
  channel ! "Hello Anna!"
  channel ! "Hola Anna!"

  sleep(7.seconds)

}
