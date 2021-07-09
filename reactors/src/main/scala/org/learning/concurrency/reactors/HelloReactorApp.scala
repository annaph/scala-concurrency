package org.learning.concurrency.reactors

import io.reactors.{JvmScheduler, Proto, Reactor, ReactorSystem}

object HelloReactorApp extends App {

  val system = ReactorSystem default "reactor-system"
  val proto = Proto[HelloReactor].withScheduler(JvmScheduler.Key.newThread)
  val channel = system spawn proto

  channel ! "Hello Anna!"

}

class HelloReactor extends Reactor[String] {

  main.events.onEvent { str =>
    log(msg = s"Received: $str")
  }

}
