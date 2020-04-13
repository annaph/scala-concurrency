package org.learing.concurrency.parallel.collections.exercises

import java.util.concurrent.atomic.AtomicLong

import org.learing.concurrency.parallel.collections.exercises.CountWhitespaceOccurrences.{generateString, timedCount, timedForeach}
import org.learing.concurrency.parallel.collections.{log, warmedTimed}

import scala.collection.parallel.CollectionConverters._
import scala.util.Random

object CountWhitespaceOccurrencesApp extends App {

  // probability
  val probabilities = (0 until 10)
    .map(_ / 9.0)
    .map(BigDecimal(_).setScale(2, BigDecimal.RoundingMode.HALF_UP))
    .map(_.doubleValue)

  val dataForeach = probabilities
    .map(p => p -> generateString(p))
    .map {
      case (p, str) =>
        val timed = timedForeach(str)
        log(s"p = $p - time = $timed")
        p -> timed
    }

  //plotGraph(dataForeach, "CountWhitespaceOccurrences", "foreach method")

  val dataCount = probabilities
    .map(p => p -> generateString(p))
    .map {
      case (p, str) =>
        val timed = timedCount(str)
        log(s"p = $p - time = $timed")
        p -> timed
    }

  //plotGraph(dataCount, "CountWhitespaceOccurrences", "count method")

}

object CountWhitespaceOccurrences {

  private val r = new Random

  private val chars = ('a' to 'z') ++ ('A' to 'Z')

  def generateString(p: Double, length: Int = 10000): Seq[Char] =
    (0 to length).map(_ => generateSymbol(p))

  def timedForeach(s: Seq[Char]): Double = {
    val count = new AtomicLong(0)

    warmedTimed(400) {
      s.par.foreach {
        case ' ' =>
          count.incrementAndGet()
        case _ =>
      }
    }
  }

  def timedCount(s: Seq[Char]): Double = {
    warmedTimed(400) {
      s.par.count(_ == ' ')
    }
  }

  def plotGraph(data: Seq[(Double, Double)], title: String, legend: String): Unit = {
    import com.quantifind.charts.Highcharts

    Highcharts.hold()
    Highcharts.line(data)
    Highcharts.title(title)
    Highcharts.legend(Seq(legend))

    Highcharts.xAxis("probability")
    Highcharts.yAxis("time (ms)")
  }

  private def generateSymbol(p: Double): Char =
    if (r.nextDouble > p) chars(r.nextInt(chars.length)) else ' '

}
