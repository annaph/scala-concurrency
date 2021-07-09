package org.learning.concurrency.reactors.exercises

import io.reactors.services.Clock
import io.reactors.{Arrayable, Channel, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.exercises.ThrottleChannel.throttle
import org.learning.concurrency.reactors.{log, sleep}

import java.time.LocalTime
import scala.collection.mutable
import scala.concurrent.duration._

object ThrottleChannelApp extends App {

  implicit val system: ReactorSystem = ReactorSystem default "reactor-system"

  val proto = Reactor[String] { self =>
    self.main.events.onEvent { str =>
      val now = LocalTime.now()
      log(msg = s"Received: $str \t[${now.getHour}:${now.getMinute}:${now.getSecond}]")
    }
  }

  val targetChannel = system spawn proto
  val throttleChannel = throttle(targetChannel)

  throttleChannel ! "Anna"
  throttleChannel ! "Stacey"
  throttleChannel ! "Nicole"

  sleep(12.second)

  system.shutdown()
  sleep(3.seconds)

}

object ThrottleChannel {

  def throttle[T](target: Channel[T])(implicit system: ReactorSystem, arr: Arrayable[T]): Channel[T] = {
    val proto = Reactor[T] { self =>
      val queue = mutable.Queue.empty[T]

      self.system.service[Clock].periodic(2.seconds).on {
        if (queue.nonEmpty) target ! queue.dequeue()
      }

      self.main.events.onEvent { t =>
        queue enqueue t
      }
    }

    system spawn proto
  }

}
