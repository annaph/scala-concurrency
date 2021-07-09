package org.learning.concurrency.reactors

import io.reactors.{Channel, Proto, Reactor, ReactorSystem}

import scala.collection.mutable
import scala.concurrent.duration._

object ChannelApp extends App {

  val system = ReactorSystem default "reactor-system"
  val proto = Proto[MapReactor[String, List[String]]]
  val mapper = system spawn proto

  mapper ! Put(key = "dns-main", value = "dns1" :: "lan" :: Nil)
  mapper ! Put(key = "dns-backup", value = "dns2" :: "com" :: Nil)

  sleep(3.seconds)

  // Client reactor
  val clientProto = Reactor[String] { self =>
    val reply = self.system.channels.daemon.open[List[String]]

    reply.events.onEvent { urls =>
      log(msg = s"URLs: ${urls mkString "|"}")
    }

    self.main.events.onMatch {
      // Process "start" message
      case "start" =>
        log(msg = "Processing 'start' message ...")
        mapper ! Get("dns-main", reply.channel)

      // Process "end" message
      case "end" =>
        log(msg = "Processing 'end' message ...")
        self.main.seal()
    }
  }

  val clientChannel = system spawn clientProto

  clientChannel ! "start"
  sleep(1.second)
  clientChannel ! "end"

  sleep(3.seconds)

}

sealed trait Operation[K, V]

class MapReactor[K, V] extends Reactor[Operation[K, V]] {

  private val map = mutable.Map.empty[K, V]

  main.events.onEvent {
    case Get(key, channel) =>
      channel ! map(key)
    case Put(key, value) =>
      map(key) = value
  }

}

case class Get[K, V](key: K, channel: Channel[V]) extends Operation[K, V]

case class Put[K, V](key: K, value: V) extends Operation[K, V]
