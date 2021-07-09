package org.learning.concurrency.reactors.protocol.serverclient

import io.reactors.protocol.{Server, _}
import io.reactors.services.Channels
import io.reactors.{Reactor, ReactorSystem}
import org.learning.concurrency.reactors.protocol.serverclient.Implicits.RequestChannelOps
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object CreatingNewConnectorApp extends App {

  implicit val system: ReactorSystem = ReactorSystem default "reactor-system"

  // Server
  val serverProto = Reactor[String] { self =>
    val channelsService = self.system.service[Channels]

    self.main.events.onMatch {
      case "terminate" =>
        self.main.seal()
    }

    channelsService
      .daemon
      .named(name = "server")
      .server[Int, Int].serve(_ * 2)
  }

  // Client
  val clientProto = Reactor[Unit] { self =>
    val channelsService = self.system.service[Channels]

    channelsService
      .await[Server.Req[Int, Int]](reactorName = "multiplier", channelName = "server")
      .onEvent { serverChannel =>
        (serverChannel ? 7) onEvent { response =>
          log(msg = s"Response: $response")
        }
      }
  }

  system spawn serverProto.withName(nm = "multiplier")
  system spawn clientProto
  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}
