package org.learning.concurrency.reactors.exercises

import io.reactors.{Channel, Proto, Reactor, ReactorSystem}
import org.learning.concurrency.reactors.exercises.BroadcastChannel.broadcast
import org.learning.concurrency.reactors.exercises.CRDTCounter.{Increment, Print, crdt}
import org.learning.concurrency.reactors.{log, sleep}

import scala.concurrent.duration._

object CRDTCounterApp extends App {

  // Create reactor system
  implicit val system: ReactorSystem = ReactorSystem default "reactor-system"

  // Create CRDT channels
  val crdtChannels = crdt

  // Send Increment requests
  crdtChannels.zipWithIndex.foreach {
    case (channel, i) =>
      channel ! Increment(i + 1)
  }

  sleep(3.seconds)

  // Print counter values
  crdtChannels.foreach(_ ! Print)
  sleep(3.seconds)

  // Shutdown reactor system
  system.shutdown()
  sleep(3.seconds)

}

object CRDTCounter {

  type CRDTChannel = (String, Channel[Request])

  def crdt(implicit system: ReactorSystem): Seq[Channel[Request]] = {
    val crdtChannels = (1 to 3)
      .map(i => s"crdt-reactor-$i")
      .map(id => id -> (system spawn Proto[CRDTReactor](params = id)))

    crdtChannels.foreach(_._2 ! AllCRDTChannels(crdtChannels))

    crdtChannels.map(_._2)
  }

  sealed trait Request

  class CRDTReactor(reactorId: String) extends Reactor[Request] {

    private implicit val sys: ReactorSystem = system

    private var counter = 0

    private var broadcastChannel: Channel[Request] = _

    main.events.onMatch {
      case AllCRDTChannels(crdtChannels) =>
        broadcastChannel = broadcast {
          crdtChannels.filter(_._1 != reactorId).map(_._2)
        }

      case Increment(n, broadcast) =>
        counter = counter + n
        if (!broadcast) broadcastChannel ! Increment(n, broadcast = true)

      case Print =>
        log(msg = s"$reactorId - $counter")
    }

  }

  case class AllCRDTChannels(channels: Seq[CRDTChannel]) extends Request

  case class Increment(n: Int, broadcast: Boolean = false) extends Request

  case object Print extends Request

}
