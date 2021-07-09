package org.learning.concurrency.reactors.service

import io.reactors.services.{Channels, Clock}
import io.reactors.{Reactor, ReactorSystem}
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object ChannelsServiceApp extends App {

  val system = ReactorSystem default "reactor-system"

  // First reactor
  val first = Reactor[Unit] { self =>
    val clockService = system.service[Clock]
    val channelsService = system.service[Channels]

    clockService.timeout(1.second).on {
      val hidden = channelsService.daemon.named(name = "hidden").open[Int]
      hidden.events.onEvent { n =>
        log(msg = s"Event received: $n")
        self.main.seal()
      }
    }
  }

  system spawn first.withName(nm = "first")

  // Second reactor
  val second = Reactor[Unit] { self =>
    val channelsService = system.service[Channels]
    channelsService.await[Int](reactorName = "first", channelName = "hidden").onEvent { channel =>
      channel ! 7
      self.main.seal()
    }
  }

  system spawn second.withName(nm = "second")

  sleep(3.seconds)

}
