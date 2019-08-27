package org.learning.concurrency.introduction.exercises

object ComposeApp {

  def compose[A, B, C](g: B => C, f: A => B): A => C =
    a => g(f(a))

  def compose1[A, B, C](g: B => C, f: A => B): A => C =
    g compose f

}
