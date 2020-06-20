package org.learning.concurrency.reactive.extensions

import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import org.learning.concurrency.reactive.extensions.RMap.{RMapDisposable, Subscribers}

import scala.collection.mutable

class RMap[K, V] {

  private val _map = mutable.Map.empty[K, V]

  private val _allSubscribers = mutable.Map.empty[K, Subscribers[V]]

  def apply(key: K): Observable[V] = Observable.create[V] { emitter =>
    val (subject, emitters) = _allSubscribers.getOrElseUpdate(
      key,
      Subject.publishSubject[V]() -> mutable.Set.empty[ObservableEmitter[V]])

    val subjectDisposable = subject.subscribe(emitter.onNext _)

    emitters += emitter
    emitter setDisposable new RMapDisposable(key, emitter, subjectDisposable, _allSubscribers)
  }

  def update(key: K, value: V): Unit = {
    _map.update(key, value)

    _allSubscribers.get(key) match {
      case Some((subject, _)) =>
        subject onNext value
      case None =>
    }

  }

  def hasSubscribers(key: K): Boolean =
    _allSubscribers contains key

}

object RMap {

  type Subscribers[V] = (Subject[V], mutable.Set[ObservableEmitter[V]])

  class RMapDisposable[K, V](key: K,
                             emitter: ObservableEmitter[V],
                             subjectDisposable: Disposable,
                             allSubscribers: mutable.Map[K, Subscribers[V]]) extends Disposable {

    private var _disposed = false

    override def dispose(): Unit = {
      allSubscribers.get(key) match {
        case Some((_, emitters)) =>
          emitters -= emitter
          subjectDisposable.dispose()
          if (emitters.isEmpty) allSubscribers -= key
          _disposed = true
        case _ =>
      }
    }

    override def isDisposed: Boolean = _disposed

  }

}
