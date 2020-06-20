package org.learning.concurrency.reactive.extensions

import org.learning.concurrency.reactive.extensions.CompositionRetry.{errorMessage, randomQuote}
import org.learning.concurrency.reactive.extensions.Observable.ObservableOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, blocking}
import scala.io.Source
import scala.util.{Failure, Success, Try}

object CompositionMapAndFilter extends App {

  val odds = Observable.interval(0.5.seconds)
    .filter(_ % 2 == 1)
    .map(n => s"odd number $n")
    .take(5)

  odds.subscribe(log, e => log(s"unexpected $e"), log("no more odds"))

  val evens =
    for {
      n <- Observable.from(0 until 9)
      if n % 2 == 0
    } yield s"even number $n"

  evens subscribe log _


  Thread sleep 7000

}

object CompositionConcatAndFlatten extends App {

  log("Using concat")
  quotes.concat.subscribe(log _)

  Thread sleep 7000

  log("Now using flatten")
  quotes.flatten.subscribe(log _)

  Thread sleep 7000

  log("Now using flatMap")
  Observable.interval(0.5.seconds).take(5)
    .flatMap(n => fetchQuoteObservable().map(txt => s"$n) $txt"))
    .subscribe(log _)

  Thread sleep 7000

  log("Now using good ol' for-comprehension")
  val qs = for {
    n <- Observable.interval(0.5.seconds).take(5)
    txt <- fetchQuoteObservable()
  } yield s"$n) $txt"

  qs subscribe log _

  Thread sleep 7000

  def quotes: Observable[Observable[String]] =
    Observable.interval(0.5.seconds)
      .take(5)
      .map(n => fetchQuoteObservable().map(txt => s"$n) $txt"))

  def fetchQuoteObservable(): Observable[String] =
    Observable from fetchQuote()

  def fetchQuote(): Future[String] = Future {
    blocking {
      val url = "http://quotes.stormconsultancy.co.uk/random.json?show_permalink=false&show_source=false"
      val file = Source.fromURL(url)

      try file.getLines().mkString finally file.close()
    }
  }

}

object CompositionConcatAndFlatten2 extends App {

  log("Using concat")
  (ob1 ++ ob2) subscribe log _

  Thread sleep 17000

  log("Now using merge")
  (ob1 merge ob2) subscribe log _

  Thread sleep 12000

  def ob1: Observable[String] =
    Observable.interval(1.seconds).map(id => s"A$id").take(7)

  def ob2: Observable[String] =
    Observable.interval(1.seconds).map(id => s"B$id").take(7)

}

object CompositionRetry extends App {

  val shortQuote =
    for {
      quote <- randomQuote
      message <- if (quote.length < 215) Observable items quote else errorMessage
    } yield message

  shortQuote.retry(7).subscribe(
    log,
    e => log(s"too long - $e"),
    log("done!"))

  def randomQuote: Observable[String] = Observable.create[String] { emitter =>
    val url = "http://quotes.stormconsultancy.co.uk/random.json?show_permalink=false&show_source=false"
    val file = Source fromURL url

    Try(file.getLines().mkString) match {
      case Success(quote) =>
        emitter onNext quote
        emitter.onComplete()
        file.close()
      case Failure(e) =>
        emitter onError e
        file.close()
    }
  }

  def errorMessage: Observable[String] =
    Observable.items("Retrying...") ++ Observable.error(new Exception)

}

object CompositionScan extends App {

  val shortQuote =
    for {
      quote <- randomQuote
      message <- if (quote.length < 215) Observable items quote else errorMessage
    } yield message

  val observable = shortQuote.retry.repeat.take(100).scan(0) {
    case (acc, "Retrying...") =>
      acc + 1
    case (acc, _) =>
      acc
  }

  observable subscribe (n => log(s"$n / 100"))

}

object CompositionErrors extends App {

  val status = Observable.create[String] { emitter =>
    emitter onNext "ok"
    emitter onNext "still ok"

    emitter onError new Exception("very bad")
  }

  val fixedStatus = status.onErrorReturn(e => e.getMessage)
  fixedStatus subscribe log _

  val continuedStatus = status.onErrorResumeNext(_ => Observable.items("better", "much better"))
  continuedStatus subscribe log _

}
