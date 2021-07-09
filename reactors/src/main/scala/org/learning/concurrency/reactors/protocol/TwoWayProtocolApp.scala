package org.learning.concurrency.reactors.protocol

import io.reactors.protocol._
import io.reactors.services.Channels
import io.reactors.{Proto, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object TwoWayProtocolApp extends App {

  val system = ReactorSystem default "reactor-system"

  val twoWayProto = Proto[TwoWayReactor]

  system spawn twoWayProto
  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}

class TwoWayReactor extends Reactor[Unit] {

  private val channelsService = system.service[Channels]

  private val twoWayProtocol = channelsService.twoWayServer[Int, String].serveTwoWay()

  // Server
  twoWayProtocol.links.onEvent { serverTwoWay =>
    serverTwoWay.input.onEvent { str =>
      log(msg = s"Server received: $str")
      serverTwoWay.output ! str.length
    }
  }

  // Client
  twoWayProtocol.channel.connect().onEvent { clientTwoWay =>
    clientTwoWay.output ! "Anna"
    clientTwoWay.output ! "Stacey"
    clientTwoWay.output ! "Victoria"

    clientTwoWay.input.onEvent { n =>
      log(msg = s"Client received: $n")
    }
  }

}
