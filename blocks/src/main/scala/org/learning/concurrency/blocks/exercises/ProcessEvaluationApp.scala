package org.learning.concurrency.blocks.exercises

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.security.Permission

import org.learning.concurrency.blocks.exercises.ProcessEvaluation._

import scala.util.{Failure, Success, Try}

object ProcessEvaluationApp extends App {

  setSecurityManager()

  val path = args(0)

  val func0: () => Any = deserializeFunc0(path)

  val result = executeFunc0(func0)

  serializeFunc0Result(result, path)

}

object ProcessEvaluation {

  def setSecurityManager(): Unit = System.setSecurityManager {
    new SecurityManager() {
      override def checkPermission(per: Permission): Unit = {}

      override def checkExit(status: Int): Unit =
        throw new SecurityException("not allowed to pass a block which contains System.exit(int)!")
    }
  }

  def deserializeFunc0(filePath: String): () => Any = {
    val inStream = new ObjectInputStream(new FileInputStream(filePath))
    Try {
      inStream.readObject.asInstanceOf[() => Any]
    } match {
      case x =>
        inStream.close()
        x match {
          case Success(func0) =>
            func0
          case Failure(e) =>
            throw new Exception("Error de-serializing function object!", e)
        }
    }
  }

  def executeFunc0(func0: () => Any): Any = Try {
    func0()
  } match {
    case Success(value) =>
      value
    case Failure(e) =>
      e
  }

  def serializeFunc0Result(result: Any, filePath: String): Unit = {
    val outStream = new ObjectOutputStream(new FileOutputStream(filePath))
    Try {
      outStream writeObject result
    } match {
      case x =>
        outStream.close()
        x match {
          case Success(_) =>
          case Failure(e) =>
            throw new Exception("Error serializing function result!", e)
        }
    }
  }

}
