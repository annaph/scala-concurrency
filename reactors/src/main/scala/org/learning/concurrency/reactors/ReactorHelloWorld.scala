package org.learning.concurrency.reactors

import io.reactors.{Reactor, ReactorSystem}

import scala.concurrent.duration._

object ReactorHelloWorld {

  def main(args: Array[String]): Unit = {
    val welcomeReactor = Reactor[String] { self =>
      self.main.events.onEvent { name =>
        log(msg = s"Welcome, $name!")
        self.main.seal()
      }
    }

    val system = ReactorSystem default "test-system"
    val channel = system spawn welcomeReactor

    channel ! "Anna"

    sleep(3.seconds)

  }

}
