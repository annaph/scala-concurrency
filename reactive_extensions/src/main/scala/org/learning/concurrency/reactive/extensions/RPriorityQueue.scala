package org.learning.concurrency.reactive.extensions

import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import org.learning.concurrency.reactive.extensions.RPriorityQueue.RPriorityQueueDisposable

import scala.collection.mutable

class RPriorityQueue[T](implicit ord: Ordering[T]) {

  private val _queue = mutable.PriorityQueue.empty[T]

  private val _subject = Subject.publishSubject[T]()

  private val _emitters = mutable.Set.empty[ObservableEmitter[T]]

  def add(t: T): Unit =
    _queue += t

  def pop(): T = {
    val t = _queue.dequeue()
    _subject onNext t

    t
  }

  def popped: Observable[T] = Observable.create[T] { emitter =>
    _emitters += emitter
    val subjectDisposable = _subject.subscribe(emitter.onNext _)

    emitter setDisposable new RPriorityQueueDisposable(emitter, subjectDisposable, _emitters)
  }

  def hasSubscribers: Boolean =
    _emitters.nonEmpty

}

object RPriorityQueue {

  class RPriorityQueueDisposable[T](emitter: ObservableEmitter[T],
                                    subjectDisposable: Disposable,
                                    allEmitters: mutable.Set[ObservableEmitter[T]]) extends Disposable {

    private var _disposed = false

    override def dispose(): Unit =
      if (allEmitters contains emitter) {
        allEmitters -= emitter
        subjectDisposable.dispose()

        _disposed = true
      }

    override def isDisposed: Boolean = _disposed

  }

}
