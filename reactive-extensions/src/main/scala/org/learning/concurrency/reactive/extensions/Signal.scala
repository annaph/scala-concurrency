package org.learning.concurrency.reactive.extensions

class Signal[T](protected val observable: Observable[T], protected val initialEvent: Option[T]) {

  private var _lastEvent = initialEvent

  observable.subscribe(t => _lastEvent = Option(t))

  def get: T = _lastEvent.get

  def map[R](f: T => R): Signal[R] = _lastEvent match {
    case Some(t) =>
      Signal(observable map f, Option(f(t)))
    case None =>
      Signal(observable.map(f))
  }

  def zip[R](other: Signal[R]): Signal[(T, R)] = (_lastEvent, other._lastEvent) match {
    case (Some(t), Some(r)) =>
      Signal(observable zip other.observable, Some(t -> r))
    case _ =>
      Signal(observable zip other.observable)
  }

  def scan[R](z: R)(f: (R, T) => R): Signal[R] =
    Signal(observable.scan(z)(f))

}

object Signal {

  def apply[T](observable: Observable[T], initialEvent: Option[T] = None): Signal[T] =
    new Signal(observable, initialEvent)

}
