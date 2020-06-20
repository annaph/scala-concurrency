package org.learning.concurrency.reactive.extensions

class RCell[T](subject: Subject[T]) extends Signal[T](Observable(subject.rxSubject), None) {

  def :=(t: T): Unit =
    subject onNext t

}

object RCell {

  def apply[T](): RCell[T] =
    new RCell(Subject.publishSubject())

}
