package org.learning.concurrency.reactors.protocol

import io.reactors.protocol.Server
import io.reactors.services.Channels
import io.reactors.{Channel, Events, ReactorSystem}

package object serverclient {

  object Implicits {

    implicit class RequestChannelOps(requestChannel: Channel[Server.Req[Int, Int]]) {

      def ?(request: Int)(implicit system: ReactorSystem): Events[Int] = {
        val channelsService = system.service[Channels]
        val responseConnector = channelsService.daemon.open[Int]

        requestChannel ! (request, responseConnector.channel)

        responseConnector.events
      }

    }

  }

}
