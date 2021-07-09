package org.learning.concurrency.reactors.exercises

import io.reactors.services.Channels
import io.reactors.{Channel, Protocol, Reactor, ReactorSystem, ReactorTerminated, Signal}
import org.learning.concurrency.reactors.exercises.ShutdownService.ShutdownSubscription
import org.learning.concurrency.reactors.{log, sleep}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration._

object ShutdownServiceApp extends App {

  val system = ReactorSystem default "reactor-system"

  // Reactor 1
  val proto1 = Reactor[String] { self =>
    val shutdownService = self.system.service[ShutdownService]

    val (subscriptionId, shutChannel) = shutdownService.subscribe
    log(msg = s"Reactor 1 subscription ID: $subscriptionId")

    self.main.events.onMatch {
      case "unsubscribe" =>
        log(msg = "Unsubscribing Reactor 1...")
        shutdownService unsubscribe subscriptionId
    }

    shutChannel.on {
      log(msg = "Releasing resources.")
      self.main.seal()
    }

  }

  val channel1 = system spawn proto1

  sleep(1.second)
  println(s"Subscriptions: ${system.service[ShutdownService].subscriptions mkString ","}")

  // Reactor 2
  val proto2 = Reactor[String] { self =>
    val shutdownService = self.system.service[ShutdownService]

    val (subscriptionId, shutChannel) = shutdownService.subscribe
    log(msg = s"Reactor 2 subscription ID: $subscriptionId")

    self.main.events.onMatch {
      case "terminate" =>
        self.main.seal()
    }

    self.sysEvents.onMatch {
      case ReactorTerminated =>
        log(msg = "Terminating Reactor 2...")
        shutdownService unsubscribe subscriptionId
    }

    shutChannel.on {
      log(msg = "Releasing resources.")
      self.main.seal()
    }

  }

  val channel2 = system spawn proto2

  sleep(1.second)
  println(s"Subscriptions: ${system.service[ShutdownService].subscriptions mkString ","}")

  // Unsubscribe Reactor 1
  channel1 ! "unsubscribe"

  sleep(1.second)
  println(s"Subscriptions: ${system.service[ShutdownService].subscriptions mkString ","}")

  // Terminate Reactor 2
  channel2 ! "terminate"

  sleep(1.second)
  println(s"Subscriptions: ${system.service[ShutdownService].subscriptions mkString ","}")

  system.shutdown()
  sleep(3.seconds)

}

class ShutdownService(val system: ReactorSystem) extends Protocol.Service {

  private val subscribers = mutable.Map.empty[String, Channel[Boolean]]

  private val lock = new AnyRef

  def subscribe: ShutdownSubscription = {
    val shut = system.service[Channels]
      .daemon
      .open[Boolean]

    val id = lock.synchronized {
      val id = ShutdownService.uniqueId
      subscribers.put(id, shut.channel)
      id
    }

    id -> shut.events.toSignal(init = false)
  }

  def unsubscribe(subscriptionId: String): Boolean =
    lock.synchronized {
      subscribers.remove(subscriptionId).isDefined
    }

  def subscriptions: List[String] =
    subscribers.keys.toList

  override def shutdown(): Unit = lock.synchronized {
    for (channel <- subscribers.values) channel ! true
  }

}

object ShutdownService {

  type ShutdownSubscription = (String, Signal[Boolean])

  def uniqueId: String =
    UUID.randomUUID().toString

}
