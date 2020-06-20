package org.learning.concurrency.reactive.extensions

import java.util.concurrent.{CompletableFuture, TimeUnit}

import io.reactivex.rxjava3.core.{ObservableEmitter, ObservableOnSubscribe, Observer, Scheduler, Observable => RxObservable}
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.{Action, BiFunction, Consumer}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

class Observable[T](val rxObservable: RxObservable[T]) {

  def subscribe(onNext: T => Unit): Disposable = {
    val f: Consumer[T] = t => onNext(t)
    rxObservable subscribe f
  }

  def subscribe(onNext: T => Unit, onError: Throwable => Unit): Disposable = {
    val f: Consumer[T] = t => onNext(t)
    val g: Consumer[Throwable] = e => onError(e)

    rxObservable.subscribe(f, g)
  }

  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onComplete: => Unit): Disposable = {
    val f: Consumer[T] = t => onNext(t)
    val g: Consumer[Throwable] = e => onError(e)
    val z: Action = () => onComplete

    rxObservable.subscribe(f, g, z)
  }

  def subscribe(observer: Observer[T]): Unit =
    rxObservable subscribe observer

  def subscribeWith[E <: Observer[T]](observer: E): E =
    rxObservable subscribeWith observer

  def withFilter(p: T => Boolean): Observable[T] =
    filter(p)

  def filter(p: T => Boolean): Observable[T] = Observable {
    val predicate: io.reactivex.rxjava3.functions.Predicate[T] = t => p(t)
    rxObservable filter predicate
  }

  def map[R](f: T => R): Observable[R] = Observable {
    val function: io.reactivex.rxjava3.functions.Function[T, R] = t => f(t)
    rxObservable map function
  }

  def flatMap[R](f: T => Observable[R]): Observable[R] = Observable {
    val function: io.reactivex.rxjava3.functions.Function[T, RxObservable[R]] = t => f(t).rxObservable
    rxObservable flatMap function
  }

  def scan[R](z: R)(f: (R, T) => R): Observable[R] = Observable {
    val function: BiFunction[R, T, R] = (r, t) => f(r, t)
    rxObservable.scan(z, function)
  }

  def take(count: Long): Observable[T] = Observable {
    rxObservable take count
  }

  def drop(count: Long): Observable[T] = Observable {
    rxObservable skip count
  }

  def distinct(): Observable[T] = Observable {
    rxObservable.distinct()
  }

  def retry: Observable[T] = Observable {
    rxObservable.retry()
  }

  def retry(times: Long): Observable[T] = Observable {
    rxObservable retry times
  }

  def repeat: Observable[T] = Observable {
    rxObservable.repeat()
  }

  def timeout(timeout: FiniteDuration): Observable[T] = Observable {
    rxObservable.timeout(timeout.toMillis, TimeUnit.MILLISECONDS)
  }

  def ++[B <: T](that: Observable[B]): Observable[T] =
    Observable(RxObservable.concat(rxObservable, that.rxObservable))

  def merge[B <: T](that: Observable[B]): Observable[T] =
    Observable(RxObservable.merge(rxObservable, that.rxObservable))

  def onErrorReturn(f: Throwable => T): Observable[T] = Observable {
    val function: io.reactivex.rxjava3.functions.Function[Throwable, T] = e => f(e)
    rxObservable onErrorReturn function
  }

  def onErrorResumeNext(f: Throwable => Observable[T]): Observable[T] = Observable {
    val function: io.reactivex.rxjava3.functions.Function[Throwable, RxObservable[T]] = e => f(e).rxObservable
    rxObservable onErrorResumeNext function
  }

  def observeOn(scheduler: Scheduler): Observable[T] = Observable {
    rxObservable observeOn scheduler
  }

  def zip[R](other: Observable[R]): Observable[(T, R)] = Observable {
    val biFunction: BiFunction[T, R, (T, R)] = (t, r) => t -> r
    rxObservable.zipWith(other.rxObservable, biFunction)
  }

  def toSignal: Signal[T] =
    Signal(this)

}

object Observable {

  def apply[T](rxObservable: RxObservable[T]): Observable[T] =
    new Observable(rxObservable)

  def items[T](item1: T): Observable[T] = Observable {
    RxObservable just item1
  }

  def items[T](item1: T, item2: T): Observable[T] = Observable {
    RxObservable.just(item1, item2)
  }

  def items[T](item1: T, item2: T, item3: T): Observable[T] = Observable {
    RxObservable.just(item1, item2, item3)
  }

  def from[T](source: Iterable[T]): Observable[T] = Observable {
    RxObservable fromIterable source.asJava
  }

  def from[T](source: Future[T])(implicit ex: ExecutionContext): Observable[T] = Observable {
    val javaFuture = new CompletableFuture[T]()

    source.onComplete {
      case Success(value) =>
        javaFuture complete value
      case Failure(e) =>
        javaFuture completeExceptionally e
    }

    RxObservable fromFuture javaFuture
  }

  def error[T](e: Throwable): Observable[T] = Observable {
    RxObservable.error[T](e)
  }

  def timer(duration: FiniteDuration): Observable[Long] = Observable {
    RxObservable.timer(duration.toMillis, TimeUnit.MILLISECONDS).map(_.longValue)
  }

  def interval(period: FiniteDuration): Observable[Long] = Observable {
    RxObservable.interval(period.toMillis, TimeUnit.MILLISECONDS).map(_.longValue)
  }

  def create[T](f: ObservableEmitter[T] => Unit): Observable[T] = Observable {
    val source: ObservableOnSubscribe[T] = emitter => f(emitter)
    RxObservable create source
  }

  def concat[T](sources: Iterable[Observable[T]]): Observable[T] = Observable {
    RxObservable concat sources.map(_.rxObservable).asJava
  }

  implicit class ObservableOps[T](observable: Observable[Observable[T]]) {

    def concat: Observable[T] = Observable {
      RxObservable concat observable.rxObservable.map(_.rxObservable)
    }

    def flatten: Observable[T] = Observable {
      RxObservable merge observable.rxObservable.map(_.rxObservable)
    }

  }

}
