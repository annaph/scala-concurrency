package org.learing.concurrency.parallel.collections.exercises

import org.learing.concurrency.parallel.collections.{ParString, log}

import scala.annotation.tailrec

object ParallelBalanceParenthesesApp extends App {

  val str1 = "0(1)(2(3))4" * 250000
  val str2 = "0)2(1(3)" * 250000
  val str3 = "0((1)2" * 250000

  log(s"str1 balanced - ${parallelBalanceParentheses(str1)}")
  log(s"str2 balanced - ${parallelBalanceParentheses(str2)}")
  log(s"str3 balanced - ${parallelBalanceParentheses(str3)}")

  def parallelBalanceParentheses(str: String): Boolean = {
    val parString = new ParString(str)

    val seqOp: (StringBuilder, Char) => StringBuilder = {
      case (acc, ch) if ch == '(' || ch == ')' =>
        acc += ch
        acc
      case (acc, _) =>
        acc
    }

    val combOp: (StringBuilder, StringBuilder) => StringBuilder = {
      case (str1, str2) =>
        balance(str1 append str2)
    }

    parString.aggregate(new StringBuilder)(seqOp, combOp).isEmpty
  }

  def balance(str: StringBuilder): StringBuilder = {
    @tailrec
    def go(i: Int, acc: List[Char]): StringBuilder = (i, acc) match {
      case (-1, _) =>
        new StringBuilder ++= acc
      case (_, Nil) =>
        go(i - 1, str.charAt(i) :: Nil)
      case (_, ')' :: xs) if str.charAt(i) == '(' =>
        go(i - 1, xs)
      case _ =>
        go(i - 1, str.charAt(i) :: acc)
    }

    go(str.length - 1, List.empty)
  }

}
