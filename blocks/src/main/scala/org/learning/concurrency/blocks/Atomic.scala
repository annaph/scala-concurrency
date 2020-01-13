package org.learning.concurrency.blocks

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import scala.annotation.tailrec

object AtomicUid extends App {

  val uid = new AtomicLong(0L)

  def uniqueId(): Long =
    uid.incrementAndGet()

  execute {
    log(s"Got a unique id asynchronously: ${uniqueId()}")
  }

  log(s"Got a unique id: ${uniqueId()}")

}

object AtomicUidCAS extends App {

  val uid = new AtomicLong(0L)

  @tailrec
  def uniqueId(): Long = {
    val oldUid = uid.get
    val newUid = oldUid + 1

    if (uid.compareAndSet(oldUid, newUid)) newUid else uniqueId()
  }

  execute {
    log(s"Got a unique id asynchronously: ${uniqueId()}")
  }

  log(s"Got a unique id: ${uniqueId()}")

}

object AtomicLock extends App {

  val lock = new AtomicBoolean(false)
  // This should be volatile variable!
  var count = 0

  def mySynchronized(body: => Unit): Unit = {
    while (lock compareAndSet(false, true)) {}

    body
    lock set false
  }

  for (_ <- 1 to 12) execute {
    mySynchronized(count += 1)
  }

  Thread sleep 3000

  log(s"Count is: $count")

}
