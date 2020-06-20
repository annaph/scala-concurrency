package org.learning.concurrency.reactive.extensions.exercises

import org.learning.concurrency.reactive.extensions.CompositionRetry.randomQuote
import org.learning.concurrency.reactive.extensions.{Observable, log}

import scala.concurrent.duration._

object AverageQuoteLengthApp extends App {

  val source = Observable
    .interval(1.seconds)
    .flatMap(_ => randomQuote)
    .scan(0D -> 0L)(quoteAccumulator)
    .drop(1)
    .map(averageLength)

  source.subscribe(avg => log(s"avg = $avg"))

  Thread sleep 17000

  def quoteAccumulator(acc: (Double, Long), quote: String): (Double, Long) = acc match {
    case (total, count) =>
      (total + quote.length) -> (count + 1)
  }

  def averageLength(data: (Double, Long)): Double = data match {
    case (total, count) => total / count
  }

}
