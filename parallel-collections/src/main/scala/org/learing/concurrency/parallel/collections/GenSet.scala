package org.learing.concurrency.parallel.collections

import scala.collection.parallel.ParSet

trait GenSet[T] {

  def contains(elem: T): Boolean

  def foreach[U](f: T => U): Unit

}

object GenSet {

  implicit def toGenSet[T](xs: Set[T]): GenSet[T] = new GenSet[T] {

    override def contains(elem: T): Boolean =
      xs contains elem

    override def foreach[U](f: T => U): Unit =
      xs foreach f

  }

  implicit def toGenSet[T](xs: ParSet[T]): GenSet[T] = new GenSet[T] {

    override def contains(elem: T): Boolean =
      xs contains elem

    override def foreach[U](f: T => U): Unit =
      xs foreach f

  }

}
