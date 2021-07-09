package org.learning.concurrency.reactors.service

import io.reactors.services.Log
import io.reactors.{Reactor, ReactorSystem}
import org.learning.concurrency.reactors.sleep

import scala.concurrent.duration._

object LoggingServiceApp extends App {

  val system = ReactorSystem default "reactor-system"

  val proto = Reactor[String] { self =>
    val logService = system.service[Log]
    logService.apply("Test reactor started!")

    self.main.seal()
  }

  system spawn proto

  sleep(3.seconds)

}
