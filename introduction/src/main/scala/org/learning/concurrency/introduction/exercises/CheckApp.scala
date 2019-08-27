package org.learning.concurrency.introduction.exercises

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object CheckApp extends App {

  @tailrec
  def check[T](xs: Seq[T])(p: T => Boolean): Boolean =
    xs match {
      case _ if xs.isEmpty =>
        true
      case _ if xs.length == 1 =>
        evalPredicate(xs(0))(p)
      case _ =>
        evalPredicate(xs(0))(p) && check(xs drop 1)(p)
    }

  private def evalPredicate[T](t: T)(p: T => Boolean): Boolean =
    Try(p(t)) match {
      case Success(x) => x
      case Failure(_) => false
    }

  println(s"${check(0 until 10)(40 / _ > 0)}")

}
