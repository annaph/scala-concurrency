package org.learing.concurrency.parallel.collections

import scala.collection.parallel.ParSeq

trait GenSeq[+T] {

  def indexWhere(p: T => Boolean): Int

  def find(p: T => Boolean): Option[T]

  def foldLeft[B](z: B)(op: (B, T) => B): B

}

object GenSeq {

  implicit def toGenSeq[T](xs: Seq[T]): GenSeq[T] = new GenSeq[T] {

    override def indexWhere(p: T => Boolean): Int =
      xs indexWhere p

    override def find(p: T => Boolean): Option[T] =
      xs find p

    override def foldLeft[B](z: B)(op: (B, T) => B): B =
      xs.foldLeft(z)(op)

  }

  implicit def toGenSeq[T](xs: ParSeq[T]): GenSeq[T] = new GenSeq[T] {

    override def indexWhere(p: T => Boolean): Int =
      xs indexWhere p

    override def find(p: T => Boolean): Option[T] =
      xs find p

    override def foldLeft[B](z: B)(op: (B, T) => B): B =
      xs.foldLeft(z)(op)

  }

}
