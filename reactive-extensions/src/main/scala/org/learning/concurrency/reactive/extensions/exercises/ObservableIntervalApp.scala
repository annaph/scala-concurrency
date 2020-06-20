package org.learning.concurrency.reactive.extensions.exercises

import org.learning.concurrency.reactive.extensions.{Observable, log}

import scala.concurrent.duration._

object ObservableIntervalApp extends App {

  val source1 = Observable.interval(5.seconds).map(_ * 5)
  val source2 = Observable.interval(12.seconds).map(_ * 12)

  (source1 merge source2)
    .filter(_ % 30 != 0)
    .distinct()
    .subscribe(n => log(s"$n"))

  Thread sleep 1000 * 63

}
