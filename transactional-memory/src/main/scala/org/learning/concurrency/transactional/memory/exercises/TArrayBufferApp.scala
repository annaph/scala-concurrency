package org.learning.concurrency.transactional.memory.exercises

import org.learning.concurrency.transactional.memory.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object TArrayBufferApp extends App {

  // 'addOne'
  {
    log("Testing 'addOne'...")

    val buffer = TArrayBuffer[Int](1)

    buffer += 1
    buffer += 2
    buffer += 3

    assert(buffer.head == 1)
    assert(buffer(1) == 2)
    assert(buffer(2) == 3)

    assert(buffer.length == 3)

    log("'addOne' works :)")
  }

  // 'addOne' concurrency
  {
    log("Testing 'addOne' concurrency ...")

    val buffer = TArrayBuffer[Int](1)

    val result = for (i <- 1 to 12) yield Future {
      Thread sleep 15
      buffer += i
    }

    Await.result(Future.sequence(result), 7.seconds)

    assert(buffer.length == 12)

    log("'addOne' concurrency works :)")
  }

  // 'prepend'
  {
    log("Testing 'prepend'...")

    val buffer = TArrayBuffer[Int](1)

    3 +=: buffer
    2 +=: buffer
    1 +=: buffer

    assert(buffer.head == 1)
    assert(buffer(1) == 2)
    assert(buffer(2) == 3)

    assert(buffer.length == 3)

    log("'prepend' works :)")
  }

  // 'prepend' concurrency
  {
    log("Testing 'prepend' concurrency...")

    val buffer = TArrayBuffer[Int](1)

    val result = for (i <- 1 to 12) yield Future {
      Thread sleep 15
      i +=: buffer
    }

    Await.result(Future.sequence(result), 7.seconds)

    assert(buffer.length == 12)

    log("'prepend' concurrency works :)")
  }

  // 'insert' & 'insertAll'
  {
    log("Testing 'insert' & 'insertAll'...")

    val buffer1 = TArrayBuffer[Int](2)

    buffer1 += 2
    buffer1 += 3
    buffer1.insert(0, 1)

    assert(buffer1.head == 1)
    assert(buffer1(1) == 2)
    assert(buffer1(2) == 3)

    assert(buffer1.length == 3)

    val buffer2 = TArrayBuffer[Int](2)

    buffer2 += 1
    buffer2.insertAll(1, List(2, 3))

    assert(buffer2.head == 1)
    assert(buffer2(1) == 2)
    assert(buffer2(2) == 3)

    assert(buffer2.length == 3)

    val buffer3 = TArrayBuffer[Int](2)

    buffer3 += 1
    buffer3 += 4
    buffer3.insertAll(1, List(2, 3))

    assert(buffer3.head == 1)
    assert(buffer3(1) == 2)
    assert(buffer3(2) == 3)
    assert(buffer3(3) == 4)

    assert(buffer3.length == 4)

    log("'insert' & 'insertAll' works :)")
  }

  // 'insert' & 'insertAll' concurrency
  {
    log("Testing 'insert' & 'insertAll' concurrency...")

    val buffer = TArrayBuffer[Int](1)

    val result = for (i <- 1 to 12) yield Future {
      Thread sleep 15
      buffer.insertAll(0, List(i, i * 10))
    }

    Await.result(Future.sequence(result), 7.seconds)

    assert(buffer.length == 24)
    for (i <- 0 until 24 by 2) assert(buffer(i + 1) == buffer(i) * 10)

    log("'insert' & 'insertAll' concurrency works :)")
  }

  // 'remove'
  {
    log("Testing 'remove'...")

    val buffer = TArrayBuffer[Int]()

    buffer += 1
    buffer += 2
    buffer += 3

    val removed = buffer remove 1

    assert(removed == 2)
    assert(buffer.head == 1)
    assert(buffer(1) == 3)

    assert(buffer.length == 2)

    log("'remove' works :)")
  }

  // 'remove' concurrency
  {
    log("Testing 'remove' concurrency ...")

    val buffer = TArrayBuffer[Int]()

    buffer.insertAll(0, 1 to 12)

    val result = for (i <- 1 to 12) yield Future {
      Thread sleep 15
      buffer remove 0
    }

    Await.result(Future.sequence(result), 7.seconds)

    assert(buffer.length == 0)

    log("'remove' concurrency works :)")
  }

  // 'remove(idx, count)'
  {
    log("Testing 'remove(idx, count)'...")

    val buffer = TArrayBuffer[Int]()

    buffer += 1
    buffer += 2
    buffer += 3
    buffer += 4
    buffer += 5
    buffer += 6
    buffer += 7

    buffer.remove(3, 2)

    assert(buffer.head == 1)
    assert(buffer(1) == 2)
    assert(buffer(2) == 3)
    assert(buffer(3) == 6)
    assert(buffer(4) == 7)
    assert(buffer.length == 5)

    buffer.remove(2, 3)

    assert(buffer.head == 1)
    assert(buffer(1) == 2)
    assert(buffer.length == 2)

    buffer.remove(0, 2)

    assert(buffer.length == 0)

    log("'remove(idx, count)' works :)")
  }

  // 'update'
  {
    log("Testing 'update'...")

    val buffer = TArrayBuffer[Int]()

    buffer += 1
    buffer += 2

    buffer.update(0, 3)

    assert(buffer.head == 3)
    assert(buffer(1) == 2)

    assert(buffer.length == 2)

    log("'update' works :)")
  }

  // 'update' concurrency
  {
    log("Testing 'update' concurrency ...")

    val buffer = TArrayBuffer[Int]()

    buffer.insertAll(0, 1 to 3)

    val result = for (i <- 0 to 2) yield Future {
      Thread sleep 15
      buffer.update(i, 100)
    }

    Await.result(Future.sequence(result), 7.seconds)

    assert(buffer.head == 100)
    assert(buffer(1) == 100)
    assert(buffer(2) == 100)

    assert(buffer.length == 3)


    log("'update' concurrency works :)")
  }

  // 'clear'
  {
    log("Testing 'clear'...")

    val buffer = TArrayBuffer[Int](1)

    buffer += 1
    buffer += 2
    buffer += 3

    buffer.clear()

    assert(buffer.length == 0)

    log("'clear' works :)")
  }

  // 'iterator'
  {
    log("Testing 'iterator'...")

    val buffer = TArrayBuffer[Int]()

    buffer += 1
    buffer += 2
    buffer += 3

    assert(buffer.toList == List(1, 2, 3))

    log("'iterator' works :)")
  }

  // 'patchInPlace'
  {
    log("Testing 'patchInPlace'...")

    val buffer = TArrayBuffer[Int]()

    buffer += 1
    buffer += 2
    buffer += 3
    buffer += 4
    buffer += 5
    buffer += 6
    buffer += 7

    buffer.patchInPlace(3, List(10, 11, 12), 2)

    assert(buffer.head == 1)
    assert(buffer(1) == 2)
    assert(buffer(2) == 3)
    assert(buffer(3) == 10)
    assert(buffer(4) == 11)
    assert(buffer(5) == 12)
    assert(buffer(6) == 6)
    assert(buffer(7) == 7)

    assert(buffer.length == 8)

    log("'patchInPlace' works :)")
  }

}
