package org.learning.concurrency.reactors.protocol

import io.reactors.protocol._
import io.reactors.services.Channels
import io.reactors.{Proto, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object RouterProtocolApp extends App {

  val system = ReactorSystem default "reactor-system"

  // Workers
  val workerChannel1 = system spawn Proto[WorkerReactor].withName(nm = "worker1")
  val workerChannel2 = system spawn Proto[WorkerReactor].withName(nm = "worker2")

  // Master
  val masterProto = Reactor[Unit] { self =>
    val channelsService = self.system.service[Channels]

    val routerProtocol = channelsService
      .daemon
      .router[String]
      .route(Router roundRobin Seq(workerChannel1, workerChannel2))

    val routerChannel = routerProtocol.channel

    routerChannel ! "one"
    routerChannel ! "two"
  }

  system spawn masterProto
  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}

class WorkerReactor extends Reactor[String] {

  main.events.onEvent { str =>
    log(msg = s"$name: $str")
  }

}
