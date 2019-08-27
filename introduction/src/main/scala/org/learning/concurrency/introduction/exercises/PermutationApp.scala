package org.learning.concurrency.introduction.exercises

object PermutationApp extends App {

  def permutations(x: String): Seq[String] =
    x match {
      case _ if x.length == 0 =>
        Seq("")
      case _ =>
        for {
          i <- 0 until x.length
          perm <- permutations(x.take(i) + x.takeRight(x.length - i - 1))
        } yield s"${x(i)}$perm"
    }

  println(s"Permutations of 'abc'\n: ${permutations("abc")}")

}
