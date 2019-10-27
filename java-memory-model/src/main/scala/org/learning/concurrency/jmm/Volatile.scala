package org.learning.concurrency.jmm

import scala.annotation.tailrec

object Volatile extends App {

  case class Page(txt: String, var position: Int)

  val pages = for (i <- 1 to 5) yield {
    val txt = List.fill(1000000 - 20 * i)("Na") mkString("", "", " Batman!")
    Page(txt, -1)
  }

  @volatile
  var found = false

  for (page <- pages) yield thread {
    @tailrec
    def find(i: Int): Unit = (found, page.txt(i)) match {
      case (true, _) =>
        ()
      case (_, '!') =>
        page.position = i
        found = true
      case _ =>
        find(i + 1)
    }

    find(0)
  }

  while (!found) {}

  log(s"results: ${pages.map(_.position)}")

}
