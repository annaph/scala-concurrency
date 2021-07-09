package org.learning.concurrency.reactors.protocol.serverclient

import io.reactors.protocol._
import io.reactors.{Reactor, ReactorSystem}
import org.learning.concurrency.reactors.protocol.serverclient.Implicits.RequestChannelOps
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object SpawningProtocolSpecificReactorDirectlyApp extends App {

  val system = ReactorSystem default "reactor-system"

  // Server
  val serverChannel = system.server[Int, Int] {
    case (state, t) =>
      2 * t
  }

  // Client
  val clientProto = Reactor[Unit] { self =>
    implicit val sys: ReactorSystem = self.system

    (serverChannel ? 7) onEvent { response =>
      log(msg = s"Server response: $response")
    }
  }

  system spawn clientProto
  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}
