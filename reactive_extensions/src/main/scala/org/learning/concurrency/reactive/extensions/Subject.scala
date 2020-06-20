package org.learning.concurrency.reactive.extensions

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.subjects.{PublishSubject, ReplaySubject, Subject => RxSubject}

class Subject[T](val rxSubject: RxSubject[T]) {

  def subscribe(onNext: T => Unit): Disposable = {
    val f: Consumer[T] = t => onNext(t)
    rxSubject subscribe f
  }

  def onNext(t: T): Unit =
    rxSubject onNext t

  def onError(e: Throwable): Unit =
    rxSubject onError e

  def onComplete(): Unit =
    rxSubject.onComplete()

  def subscribe(other: Subject[T]): Unit =
    rxSubject subscribe other.rxSubject

  def toSignal: Signal[T] =
    Signal(Observable(this.rxSubject))

}

object Subject {

  def apply[T](rxSubject: RxSubject[T]): Subject[T] =
    new Subject(rxSubject)

  def publishSubject[T](): Subject[T] = Subject {
    PublishSubject.create[T]()
  }

  def replaySubject[T](): Subject[T] = Subject {
    ReplaySubject.create[T]()
  }

  def toDisposableObserver[T](subject: Subject[T]): DisposableObserver[T] = new DisposableObserver[T] {
    override def onNext(t: T): Unit =
      subject onNext t

    override def onError(e: Throwable): Unit =
      subject onError e

    override def onComplete(): Unit =
      subject.onComplete()
  }

}
