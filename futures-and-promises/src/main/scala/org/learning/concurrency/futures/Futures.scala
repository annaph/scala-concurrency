package org.learning.concurrency.futures

import java.io.{File, FileNotFoundException}

import org.apache.commons.io.FileUtils
import org.learning.concurrency.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object FuturesComputation extends App {

  Future {
    log("the future is here")
  }

  log("the future is coming")

  Thread sleep 3000

}

object FuturesDataType extends App {

  val buildFile: Future[String] = Future {
    val file = Source fromFile "build.sbt"
    Try {
      file.getLines() mkString "\n"
    } match {
      case x =>
        file.close()
        x match {
          case Success(str) =>
            str
          case Failure(e) =>
            s"Error reading 'build.sbt' file: ${e.getMessage}"
        }
    }
  }

  log("started reading the build file asynchronously")
  log(s"status: ${buildFile.isCompleted}")

  Thread sleep 250

  log(s"status: ${buildFile.isCompleted}")
  log(s"build file content:\n${buildFile.value}")

}

object FuturesCallbacks extends App {

  val urlSpec = getUrlSpec

  def getUrlSpec: Future[Seq[String]] = Future {
    val f = Source fromURL "http://www.w3.org/Addressing/URL/url-spec.txt"
    try f.getLines().toSeq finally f.close()
  }

  def find(lines: Seq[String], keyword: String): String =
    lines.zipWithIndex.collect {
      case (line, i) if line contains keyword =>
        i -> line
    }.mkString("\n")

  urlSpec.foreach {
    lines => {
      val results = find(lines, "telnet")
      log(s"Found occurrences of 'telnet'\n$results\n")
    }
  }

  urlSpec.foreach {
    lines => {
      val results = find(lines, "password")
      log(s"Found occurrences of 'password'\n$results\n")
    }
  }

  log("callbacks installed, continuing with other work")

  Thread sleep 12000

}

object FuturesFailure extends App {

  def getUrlSpec: Future[Seq[String]] = Future {
    val f = Source fromURL "http://www.w3.org/Addressing/URL/non-existingurl-spec.txt"
    f.getLines().toSeq
  }

  getUrlSpec.failed.foreach {
    e => log(s"exception occurred - $e")
  }

  Thread sleep 12000

}

object FuturesTry extends App {

  val threadName = Try {
    Thread.currentThread.getName
  }

  val someText = Try {
    "Try object are created synchronously"
  }

  val message: Try[String] = for {
    name <- threadName
    text <- someText
  } yield s"$text, t = $name"

  message match {
    case Success(msg) =>
      log(msg)
    case Failure(e) =>
      log(s"There should be no $e here.")
  }

  Thread sleep 3000

}

object FuturesExceptions extends App {

  val file: Future[String] = Future {
    val file = Source.fromFile(".gitignore-SAMPLE")
    try file.getLines().mkString("\n") finally file.close()
  }

  file foreach log

  file.failed.foreach {
    case e: FileNotFoundException =>
      log(s"Cannot find file - $e")
    case e =>
      log(s"Failed due to $e")
  }

  file.onComplete {
    case Success(text) =>
      log(text)
    case Failure(e) =>
      log(s"Failed due to $e")
  }

  Thread sleep 3000

}

object FuturesNonFatal extends App {
  val f = Future {
    throw new InterruptedException
  }

  val g = Future {
    throw new IllegalArgumentException
  }

  f.failed.foreach {
    e => log(s"error - $e")
  }

  g.failed.foreach {
    g => log(s"error - $g")
  }


  Thread sleep 3000

}

object FuturesClumsyCallback extends App {

  def blacklistPatterns(filename: String): Future[List[String]] = Future {
    val file = Source fromFile filename
    try {
      file.getLines()
        .filter(!_.isEmpty)
        .filter(!_.startsWith("#"))
        .toList
    } finally file.close()
  }

  def findFiles(patterns: List[String]): List[String] = {
    val root = new File(".")
    for {
      file <- FileUtils.iterateFiles(root, null, true).asScala.toList
      pattern <- patterns
      filePath = file.getCanonicalPath
      if filePath startsWith (root.getCanonicalPath + File.separator + pattern)
    } yield filePath
  }

  blacklistPatterns("blacklist").foreach {
    patterns => {
      val matches = findFiles(patterns)
      log(s"matches: ${matches mkString "\n"}")
    }
  }

  Thread sleep 3000

}

object FuturesMap extends App {

  val buildFile: Future[Seq[String]] = Future {
    val file = Source fromFile "build.sbt"
    try file.getLines().toSeq finally file.close()
  }

  val longestBuildLine: Future[String] =
    for {
      lines <- buildFile
    } yield lines.maxBy(_.length)

  longestBuildLine.onComplete {
    case Success(line) =>
      log(s"The longest build line is '$line'")
    case Failure(e) =>
      log(s"Error finding longest build line $e")
  }

  Thread sleep 3000

}

object FuturesFlatMap extends App {

  val netiquette = Future {
    val file = Source fromURL "https://www.ietf.org/rfc/rfc1855.txt"
    try file.getLines().mkString finally file.close
  }

  val urlSpec = Future {
    val file = Source fromURL "https://www.w3.org/Addressing/URL/url-spec.txt"
    try file.getLines().mkString finally file.close()
  }

  val answer =
    for {
      netText <- netiquette
      urlText <- urlSpec
    } yield "First read this: " + netText + ". Now try this: " + urlText

  answer foreach log

  Thread sleep 12000

}

object FuturesDifferentFlatMap extends App {

  val answer =
    for {
      netText <- Future {
        val file = Source fromURL "https://www.ietf.org/rfc/rfc1855.txt"
        try file.getLines().mkString finally file.close
      }
      urlText <- Future {
        val file = Source fromURL "https://www.w3.org/Addressing/URL/url-spec.txt"
        try file.getLines().mkString finally file.close()
      }
    } yield "First read this: " + netText + ". Now try this: " + urlText

  answer foreach log

  Thread sleep 12000

}

object FuturesRecover extends App {

  val netiquetteUrl = "http://www.ietf.orgXXX/rfc/rfc1855.doc"

  val netiquette = Future {
    val file = Source fromURL netiquetteUrl
    try file.getLines().mkString finally file.close
  }.recover {
    case _: Throwable =>
      "Dear boss, thank for your email. " +
        "You might be interested to known that ftp links " +
        "can also point to regular files we keep on our servers."
  }

  netiquette foreach log

  Thread sleep 12000

}
