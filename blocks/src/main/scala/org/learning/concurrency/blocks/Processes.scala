package org.learning.concurrency.blocks

import scala.sys.process._

object ProcessRun extends App {

  val command = "ls"
  val exitCode = command.!

  log(s"command exited with status $exitCode")

}

object ProcessLineCount extends App {

  val lc = lineCount("build.sbt")

  def lineCount(filename: String): Int = {
    val command = s"""wc "$filename""""
    val output = command.!!

    output.trim().split(' ').head.toInt
  }

  log(s"File build.sbt has $lc lines.")

}

object ProcessAsync extends App {

  val command = "ls -R /"
  val lsProcess = command.run()

  Thread sleep 1000

  log("Timeout - killing ls!")
  lsProcess.destroy()

}
