package org.learning.concurrency.reactors.protocol.serverclient

import io.reactors.services.Channels
import io.reactors.{Arrayable, Channel, Events, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.protocol.serverclient.CustomServerClientProtocol.{RequestChannelOps, server}
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object CustomServerClientProtocolApp extends App {

  val system: ReactorSystem = ReactorSystem default "reactor-system"

  val serverClientProto = Reactor[Unit] { self =>
    implicit val sys: ReactorSystem = self.system
    val requestChannel = server[String, String](_.toUpperCase)

    (requestChannel ? "anna") onEvent { response =>
      log(msg = s"Server response: $response")
    }
  }

  system spawn serverClientProto
  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}

object CustomServerClientProtocol {

  type Request[T, S] = (T, Channel[S])

  type RequestChannel[T, S] = Channel[Request[T, S]]

  def server[T, S](f: T => S)(implicit system: ReactorSystem): RequestChannel[T, S] = {
    val channelsService = system.service[Channels]
    val requestConnector = channelsService.open[Request[T, S]]

    requestConnector.events.onMatch {
      case (t, responseChannel) =>
        responseChannel ! f(t)
    }

    requestConnector.channel
  }

  implicit class RequestChannelOps[T, S: Arrayable](requestChannel: RequestChannel[T, S]) {

    def ?(request: T)(implicit system: ReactorSystem): Events[S] = {
      val channelsService = system.service[Channels]
      val responseConnector = channelsService.daemon.open[S]

      requestChannel ! (request, responseConnector.channel)

      responseConnector.events
    }

  }

}
