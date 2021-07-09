package org.learning.concurrency.reactors.service

import io.reactors.services.Clock
import io.reactors.{Reactor, ReactorSystem}
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object ClockServiceApp extends App {

  val system = ReactorSystem default "reactor-system"

  val proto = Reactor[String] { self =>
    val clockService = system.service[Clock]

    clockService.timeout(1.second).on {
      log(msg = "done!")
      self.main.seal()
    }
  }

  system spawn proto

  sleep(3.seconds)

}
