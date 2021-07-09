package org.learning.concurrency.reactors.exercises

import io.reactors.{Arrayable, Channel, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.exercises.BroadcastChannel.broadcast
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object BroadcastChannelApp extends App {

  // Reactor system
  implicit val system: ReactorSystem = ReactorSystem default "reactor-system"

  // Reactor 1
  val proto1 = Reactor[String] { self =>
    self.main.events.onEvent(str => log(msg = s"Reactor 1: $str"))
  }

  // Reactor 2
  val proto2 = Reactor[String] { self =>
    self.main.events.onEvent(str => log(msg = s"Reactor 2: $str"))
  }

  // Reactor 3
  val proto3 = Reactor[String] { self =>
    self.main.events.onEvent(str => log(msg = s"Reactor 3: $str"))
  }

  val targetChannel1 = system spawn proto1
  val targetChannel2 = system spawn proto2
  val targetChannel3 = system spawn proto3

  // Broadcast
  val broadcastChannel = broadcast(List(targetChannel1, targetChannel2, targetChannel3))

  broadcastChannel ! "Anna"
  broadcastChannel ! "Stacey"
  broadcastChannel ! "Nicole"

  sleep(7.seconds)

  system.shutdown()
  sleep(3.seconds)

}

object BroadcastChannel {

  def broadcast[T](targets: Seq[Channel[T]])(implicit system: ReactorSystem, arr: Arrayable[T]): Channel[T] = {
    val proto = Reactor[T] { self =>
      self.main.events.onEvent { event =>
        targets.foreach(_ ! event)
      }
    }

    system spawn proto
  }

}
