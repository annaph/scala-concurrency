package org.learning.concurrency.introduction.exercises

object FuseApp {

  def fuse[A, B](a: Option[A], b: Option[B]): Option[(A, B)] =
    for {
      x <- a
      y <- b
    } yield x -> y

}
