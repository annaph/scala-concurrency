package org.learning.concurrency.reactors.service

import io.reactors.services.Channels
import io.reactors.{Channel, Protocol, Reactor, ReactorSystem, Signal}
import org.learning.concurrency.reactors.{log, sleep}

import scala.collection.mutable
import scala.concurrent.duration._

object CustomServiceApp extends App {

  val system = ReactorSystem default "reactor-system"

  val proto = Reactor[Unit] { self =>
    val shutdownService = system.service[ShutdownService]
    shutdownService.state.on {
      log(msg = "Releasing resources.")
      self.main.seal()
    }
  }

  system spawn proto
  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}

class ShutdownService(val system: ReactorSystem) extends Protocol.Service {

  private val subscribers = mutable.Set.empty[Channel[Boolean]]

  private val lock = new AnyRef

  def state: Signal[Boolean] = {
    val channelsService = system.service[Channels]
    val shut = channelsService.daemon.open[Boolean]

    lock.synchronized {
      subscribers += shut.channel
    }

    shut.events.toSignal(init = false)
  }

  override def shutdown(): Unit = lock.synchronized {
    for (channel <- subscribers) channel ! true
  }

}
