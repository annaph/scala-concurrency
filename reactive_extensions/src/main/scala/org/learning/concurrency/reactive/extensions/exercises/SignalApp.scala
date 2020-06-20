package org.learning.concurrency.reactive.extensions.exercises

import org.learning.concurrency.reactive.extensions.Subject

object SignalApp extends App {

  // get
  val subject1 = Subject.publishSubject[Int]()
  val signal1 = subject1.toSignal
  subject1 onNext 1
  assert(signal1.get == 1)
  subject1 onNext 2
  assert(signal1.get == 2)

  // map
  val subject2 = Subject.publishSubject[Int]()
  val signal2 = subject2.toSignal
  val increment = signal2.map(_ + 1)
  subject2 onNext 1
  assert(signal2.get == 1)
  assert(increment.get == 2)
  subject2 onNext 2
  assert(signal2.get == 2)
  assert(increment.get == 3)

  // zip
  val subject31 = Subject.publishSubject[Int]()
  val subject32 = Subject.publishSubject[String]()
  val signal31 = subject31.toSignal
  val signal32 = subject32.toSignal
  subject31 onNext 1
  subject32 onNext "A"
  val zipped = signal31 zip signal32
  assert(zipped.get == 1 -> "A")
  subject31 onNext 2
  subject32 onNext "B"
  assert(zipped.get == 2 -> "B")

  // scan
  val subject4 = Subject.publishSubject[Int]()
  val signal4 = subject4.toSignal
  subject4 onNext 0
  val scanned = signal4.scan(10)(_ + _)
  assert(scanned.get == 10)
  subject4 onNext 2
  assert(scanned.get == 12)
  subject4 onNext 3
  assert(scanned.get == 15)

}
