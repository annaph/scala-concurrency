package org.learning.concurrency.reactive.extensions.exercises

import org.learning.concurrency.reactive.extensions.RCell

object RCellApp extends App {

  // get
  val rCell1 = RCell[Int]()
  rCell1 := 1
  assert(rCell1.get == 1)

  // map
  val rCell2 = RCell[Int]()
  val increment = rCell2.map(_ + 1)
  rCell2 := 1
  assert(increment.get == 2)
  rCell2 := 2
  assert(increment.get == 3)

  // zip
  val rCell31 = RCell[Int]()
  val rCell32 = RCell[String]()
  val zipped = rCell31 zip rCell32
  rCell31 := 1
  rCell32 := "A"
  assert(zipped.get == 1 -> "A")
  rCell31 := 2
  rCell32 := "B"
  assert(zipped.get == 2 -> "B")

  // scan
  val rCell4 = RCell[Int]()
  val scanned = rCell4.scan(10)(_ + _)
  rCell4 := 0
  assert(scanned.get == 10)
  rCell4 := 2
  assert(scanned.get == 12)
  rCell4 := 3
  assert(scanned.get == 15)

}
