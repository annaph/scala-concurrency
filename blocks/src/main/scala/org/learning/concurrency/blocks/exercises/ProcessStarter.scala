package org.learning.concurrency.blocks.exercises

import java.io._
import java.util.regex.Pattern

import org.learning.concurrency.blocks.log

import scala.sys.process.Process
import scala.util.{Failure, Success, Try}

class ProcessStarter {

  def spawn[T](block: => T): T = {
    implicit val tmpFile: File = File.createTempFile("concurrent-programming-in--scala-", null)

    serializeFunc0(() => block)
    evaluateFunc0
    deserializeFunc0Result
  }

  private def serializeFunc0[T](func0: () => T)(implicit tmpFile: File): Unit = {
    val outStream = new ObjectOutputStream(new FileOutputStream(tmpFile))
    Try {
      outStream writeObject func0
    } match {
      case x =>
        outStream.close()
        x match {
          case Success(_) =>
          case Failure(e) =>
            tmpFile.delete()
            throw new Exception("Error serializing function object!", e)
        }
    }
  }

  private def evaluateFunc0(implicit tmpFile: File): Unit = {
    val className = ProcessEvaluationApp.getClass.getName.split(Pattern quote "$")(0)
    val command = s"java -cp ${System getProperty "java.class.path"} $className ${tmpFile.getCanonicalPath}"
    Try {
      log(s"Executing command: '$command'")
      Process(command).!
    } match {
      case Success(_) =>
      case Failure(e) =>
        tmpFile.delete()
        throw new Exception("Fails to evaluate block in a new JVM process!", e)
    }
  }

  private def deserializeFunc0Result[T](implicit tmpFile: File): T = {
    val inStream = new ObjectInputStream(new FileInputStream(tmpFile))
    Try {
      inStream.readObject
    } match {
      case x =>
        tmpFile.delete()
        inStream.close()
        x match {
          case Success(ex: Throwable) =>
            throw ex
          case Success(result) =>
            result.asInstanceOf[T]
          case Failure(e) =>
            throw new Exception("Error de-serializing function result!", e)
        }
    }
  }

}

object ProcessStarterApp extends App {

  val processStarter = new ProcessStarter

  val s = processStarter.spawn {
    1 + 1
  }

  log(s"s = $s")
  assert(s == 2)

  Try {
    processStarter.spawn {
      "test".toInt
    }
  } match {
    case Failure(e: NumberFormatException) =>
      log(s"Caught '$e'")
    case _ =>
      assert(false)
  }

  Try {
    processStarter.spawn {
      System exit 0
    }
  } match {
    case Failure(e: SecurityException) =>
      log(s"Caught '$e'")
    case _ =>
      assert(false)
  }

}
