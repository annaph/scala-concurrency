package org.learning.concurrency.reactors.exercises

import io.reactors.{Arrayable, Channel, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.exercises.TwiceChannel.twice
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object TwiceChannelApp extends App {

  implicit val system: ReactorSystem = ReactorSystem default "reactor-system"

  val proto = Reactor[String] { self =>
    self.main.events.onEvent { str =>
      log(msg = s"Received: $str")
    }
  }

  val targetChannel = system spawn proto
  val twiceChannel = twice(targetChannel)

  twiceChannel ! "Anna"
  twiceChannel ! "Stacey"
  twiceChannel ! "Nicole"

  sleep(1.second)

  system.shutdown()
  sleep(3.seconds)

}

object TwiceChannel {

  def twice[T](target: Channel[T])(implicit system: ReactorSystem, arr: Arrayable[T]): Channel[T] = {
    val proto = Reactor[T] { self =>
      self.main.events.onEvent { t =>
        target ! t
        target ! t
      }
    }

    system spawn proto
  }

}
