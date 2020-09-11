package org.learning.concurrency.transactional.memory.exercises

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.stm.{InTxn, Ref, TArray, atomic}
import scala.reflect.ClassTag

class TArrayBuffer[T](initialSize: Int)(implicit cm: ClassTag[T]) extends mutable.Buffer[T] {

  private val _length = Ref(0)

  private val _array = Ref(TArray.ofDim[T](initialSize))

  override def apply(i: Int): T = atomic { implicit txn =>
    if (i < 0 || i >= _length()) throw new IndexOutOfBoundsException(i.toString)

    val arr = _array()
    arr(i)
  }

  override def addOne(elem: T): TArrayBuffer.this.type = atomic { implicit txn =>
    val newLength = _length() + 1
    if (newLength >= _array().length) TArrayBuffer.copyToNewArray(_array, newLength)

    val arr = _array()
    val index = _length()
    arr(index) = elem

    _length update newLength

    this
  }

  override def prepend(elem: T): TArrayBuffer.this.type = atomic { implicit txn =>
    val newLength = _length() + 1
    if (newLength >= _array().length) TArrayBuffer.copyToNewArray(_array, newLength)

    val arr = _array()

    TArrayBuffer.shiftRight(0, 1, arr, newLength)

    arr(0) = elem
    _length update newLength

    this
  }

  override def insert(idx: Int, elem: T): Unit =
    insertAll(idx, List(elem))

  override def insertAll(idx: Int, elems: IterableOnce[T]): Unit = atomic { implicit txn =>
    if (idx < 0 || idx > _length()) throw new IndexOutOfBoundsException(idx.toString)

    val size = elems.iterator.size
    val newLength = _length() + size
    if (newLength >= _array().length) TArrayBuffer.copyToNewArray(_array, newLength)

    val arr = _array()

    if (idx != _length()) TArrayBuffer.shiftRight(idx, size, arr, newLength)

    elems.iterator.zipWithIndex.foreach {
      case (t, i) =>
        arr(idx + i) = t
    }

    _length update newLength

    this
  }

  override def length: Int =
    _length.single()

  override def remove(idx: Int): T = atomic { implicit txn =>
    if (idx < 0 || idx > _length()) throw new IndexOutOfBoundsException(idx.toString)

    val arr = _array()
    val result = arr(idx)

    val newLength = _length() - 1

    TArrayBuffer.shiftLeft(idx, 1, arr, _length())

    arr(newLength) = null.asInstanceOf[T]
    _length update newLength

    result
  }

  override def remove(idx: Int, count: Int): Unit = atomic { implicit txn =>
    if (count < 0) throw new IllegalArgumentException(count.toString)
    if (idx < 0 || idx > (_length() - count)) throw new IndexOutOfBoundsException(idx.toString)

    val arr = _array()
    val newLength = _length() - count

    TArrayBuffer.shiftLeft(idx, count, arr, _length())

    for (i <- newLength until _length()) arr(i) = null.asInstanceOf[T]
    _length update newLength
  }

  override def update(idx: Int, elem: T): Unit = atomic { implicit txn =>
    if (idx < 0 || idx >= _length()) throw new IndexOutOfBoundsException(idx.toString)

    val arr = _array()
    arr(idx) = elem
  }

  override def patchInPlace(from: Int, patch: IterableOnce[T], replaced: Int): TArrayBuffer.this.type = atomic { implicit txn =>
    if (from < 0 || from > _length()) throw new IndexOutOfBoundsException(from.toString)
    
    remove(from, replaced)
    insertAll(from, patch)

    this
  }

  override def iterator: Iterator[T] = atomic { implicit txn =>
    val arr = _array()
    val seq = for (i <- 0 until _length()) yield arr(i)

    seq.iterator
  }

  override def clear(): Unit = atomic { implicit txn =>
    _array update TArray.ofDim[T](initialSize)
    _length update 0
  }

}

object TArrayBuffer {

  def apply[T](initialSize: Int = 8)(implicit cm: ClassTag[T]): TArrayBuffer[T] =
    new TArrayBuffer(initialSize)(cm)

  private def copyToNewArray[T](array: Ref[TArray[T]], length: Int)
                               (implicit txn: InTxn, cm: ClassTag[T]): Unit = {
    val newLength: Long = {
      @tailrec
      def go(l: Long): Long =
        if (length > l) go(2 * l) else l

      val result = go(2 * array().length)
      if (result > Int.MaxValue) Int.MaxValue else result
    }

    val newArray = TArray.ofDim[T](newLength.toInt)
    array().refs.zipWithIndex.foreach {
      case (t, i) => newArray(i) = t()
    }

    array update newArray
  }

  private def shiftRight[T](src: Int, dst: Int, array: TArray[T], count: Int)(implicit txn: InTxn): Unit =
    for (i <- (count - 1) to (src + dst) by -1) {
      array(i) = array(i - dst)
    }

  private def shiftLeft[T](src: Int, dst: Int, array: TArray[T], count: Int)(implicit txn: InTxn): Unit =
    for (i <- (src + dst) until count) {
      array(i - dst) = array(i)
    }

}
