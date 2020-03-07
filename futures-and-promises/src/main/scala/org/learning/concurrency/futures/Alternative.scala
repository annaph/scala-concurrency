package org.learning.concurrency.futures

import org.learning.concurrency.log
import scalaz.concurrent.Future

import scala.util.Random

object Alternative extends App {

  val tombola = Future {
    Random shuffle (0 until 10000).toVector
  } //.unsafeStart

  tombola.unsafePerformAsync { numbers =>
    log(s"And the winner is: ${numbers.head}")
  }

  tombola.unsafePerformAsync { numbers =>
    log(s"... ahem, winner is: ${numbers.head}")
  }

  Thread sleep 3000

}
