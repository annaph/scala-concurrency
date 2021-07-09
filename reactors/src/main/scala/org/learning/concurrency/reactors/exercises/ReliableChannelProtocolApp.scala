package org.learning.concurrency.reactors.exercises

import io.reactors.services.{Channels, Clock}
import io.reactors.{Arrayable, Channel, Connector, Events, Proto, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.exercises.ReliableChannelProtocol.{Req, reliableServer}
import org.learning.concurrency.reactors.{log, sleep}

import java.time.LocalDateTime
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, blocking}
import scala.reflect.ClassTag
import scala.util.Random

object ReliableChannelProtocolApp extends App {

  // Reactor system
  val system = ReactorSystem default "reactor-system"

  // Client
  val client = system spawn Proto[ClientReactor]

  client ! "start"
  sleep(24.seconds)

  system.shutdown()
  sleep(3.seconds)

}

class ClientReactor extends Reactor[String] {

  implicit private val sys: ReactorSystem = system

  implicit private val or: Ordering[Event] = Event.EventOrdering

  implicit private val et: Event => LocalDateTime = e => e.eventTime

  private val random = new Random

  main.events onMatch {
    // Open reliable connection
    case "start" =>
      reliableServer[Event].openReliable.onEvent(startProtocol)
  }

  // Start protocol
  private def startProtocol(req: Req[Event]): Unit = req match {
    case (requestChannel, responseEvents) =>
      // Send events to sever
      system.service[Clock].countdown(17, 300.milliseconds).onEvent { id =>
        val event = Event(msg = s"Event ${17 - id}", eventTime = LocalDateTime.now())
        sendEvent(event, requestChannel)
      }
      // Receive events from server
      responseEvents.onEvent { str =>
        val now = LocalDateTime.now()
        log(msg = s"Received at ${now.getHour}:${now.getMinute}:${now.getSecond} ===> $str")
      }
  }

  // Send event
  private def sendEvent(event: Event, channel: Channel[Event]): Future[Unit] = Future {
    blocking {
      val delay = 100 + (random nextInt 2500)
      Thread sleep delay
      channel ! event
    }
  }

}

class ReliableChannelProtocol[T](system: ReactorSystem, eventTime: T => LocalDateTime)
                                (implicit arr: Arrayable[T], ct: ClassTag[T], or: Ordering[T]) {
  // Watermark interval
  private val watermarkInterval = 2.second

  // Protocol connector
  private val protocolConnector: Connector[Req[T]] = system
    .service[Channels]
    .open[Req[T]]

  // Protocol reactor
  private val protocol = system spawn Reactor[String] { self =>
    self.main.events onMatch {
      case "open" =>
        val clientConnector = system.service[Channels].open[T]
        val serverChannel = system spawn server(clientConnector.channel)

        val req = serverChannel -> clientConnector.events
        protocolConnector.channel ! req
    }
  }

  // Open connection
  def openReliable: Events[Req[T]] = {
    protocol ! "open"
    protocolConnector.events
  }

  // Server reactor
  private def server(clientChannel: Channel[T]): Proto[Reactor[T]] = Reactor[T] { self =>
    // Create Sorting reactor
    val sortingReactor = Proto[SortingReactor[T]](clientChannel, or)
    val sortingChannel = system spawn sortingReactor

    // Create Watermark reactor
    val watermarkReactor = Proto[WatermarkReactor[T]](watermarkInterval, eventTime, sortingChannel, ct)
    val watermarkChannel = system spawn watermarkReactor

    // Process input events
    self.main.events.onEvent { t =>
      watermarkChannel ! t
    }
  }

}

object ReliableChannelProtocol {

  type Req[T] = (Channel[T], Events[T])

  def reliableServer[T](implicit system: ReactorSystem,
                        eventTime: T => LocalDateTime,
                        arr: Arrayable[T],
                        ct: ClassTag[T],
                        or: Ordering[T]): ReliableChannelProtocol[T] =
    new ReliableChannelProtocol[T](system, eventTime)

}

class WatermarkReactor[T](watermarkInterval: Duration,
                          eventTime: T => LocalDateTime,
                          sortingChannel: Channel[Seq[T]],
                          implicit val ct: ClassTag[T]) extends Reactor[T] {

  private var currWatermark = LocalDateTime.now()
  private var prevWatermark = currWatermark minusSeconds watermarkInterval.toSeconds

  private var prevBucket = mutable.ArrayBuffer.empty[T]
  private var currBucket = mutable.ArrayBuffer.empty[T]

  // Process input events
  main.events.onEvent { event =>
    val time = eventTime(event)

    if (time isBefore prevWatermark) log(msg = s"Dropping event ===> $event")
    else if (time isAfter currWatermark) currBucket append event
    else prevBucket append event
  }

  // Update watermarks and buckets
  system.service[Clock].periodic(watermarkInterval).on {
    prevWatermark = currWatermark
    currWatermark = LocalDateTime.now()

    val arr = Array.ofDim[T](prevBucket.size)
    prevBucket.copyToArray(arr)

    prevBucket = currBucket
    currBucket = mutable.ArrayBuffer.empty[T]

    if (arr.nonEmpty) sortingChannel ! arr
  }
}

class SortingReactor[T](clientChannel: Channel[T], implicit val or: Ordering[T]) extends Reactor[Seq[T]] {
  // Sort bucket and emit
  main.events.onEvent { bucket =>
    bucket.sorted.foreach(clientChannel ! _)
  }
}

case class Event(msg: String, eventTime: LocalDateTime) {

  override def toString: String =
    s"$msg - ${eventTime.getHour}:${eventTime.getMinute}:${eventTime.getSecond}"

}

object Event {

  implicit object EventOrdering extends Ordering[Event] {

    override def compare(event1: Event, event2: Event): Int = {
      if (event1.eventTime isBefore event2.eventTime) -1
      else if (event1.eventTime isAfter event2.eventTime) 1
      else 0
    }

  }

}
