package org.learning.concurrency.blocks

import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.io.FileUtils

import scala.annotation.tailrec
import scala.collection.concurrent
import scala.jdk.CollectionConverters._

object FileSystemApp extends App {

  val fileSystem = FileSystem(".")

  fileSystem logMessage "Testing log!"

  fileSystem deleteFile "test.txt"

  val rootFiles = fileSystem.allFiles
  log(s"All files in the root dir: ${rootFiles mkString ", "}")

  Thread sleep 3000

}

sealed trait State

case class Copying(n: Int) extends State

case object Idle extends State

case object Creating extends State

case object Deleting extends State

case class Entry(isDir: Boolean,
                 state: AtomicReference[State] = new AtomicReference[State](Idle))

case class FileSystem(root: String) {

  private val messages = new LinkedBlockingQueue[String]()

  private val files: concurrent.Map[String, Entry] = concurrent.TrieMap.empty[String, Entry]

  for (file <- FileUtils.iterateFiles(new File(root), null, false).asScala)
    files put(file.getName, Entry(isDir = false))

  FileSystem.logger(messages).start()

  def copyFile(srcFilename: String, destFilename: String): Unit =
    FileSystem copyFile(this, files, srcFilename, destFilename)

  def deleteFile(fileName: String): Unit =
    FileSystem deleteFile(this, files, fileName)

  def allFiles: Iterable[String] =
    FileSystem allFiles files

  def logMessage(msg: String): Unit =
    messages offer msg

}

object FileSystem {

  private def logger(messages: LinkedBlockingQueue[String]): Thread = new Thread {
    this setDaemon true

    override def run(): Unit =
      while (true) {
        val msg = messages.take()
        log(msg)
      }

  }

  private def copyFile(fileSystem: FileSystem,
                       files: concurrent.Map[String, Entry],
                       srcFilename: String,
                       destFilename: String): Unit = {
    def copy(srcEntry: Entry): Unit = execute {
      if (acquire(fileSystem, srcEntry)) {
        val destEntry = Entry(isDir = false, state = new AtomicReference(Creating))
        files.putIfAbsent(destFilename, destEntry) match {
          case None =>
            FileUtils.copyFile(new File(srcFilename), new File(destFilename))
            release(fileSystem, destEntry)
          case _ =>
        }

        release(fileSystem, srcEntry)
      }
    }

    files.get(srcFilename) match {
      case Some(srcEntry) if !srcEntry.isDir =>
        copy(srcEntry)
      case Some(_) =>
        fileSystem logMessage s"Path '$srcFilename' is a directory!"
      case None =>
        fileSystem logMessage s"File '$srcFilename' does not exist."
    }
  }

  private def deleteFile(fileSystem: FileSystem,
                         files: concurrent.Map[String, Entry],
                         filename: String): Unit = {
    def delete(entry: Entry): Unit = execute {
      if (prepareForDelete(fileSystem, entry)) {
        FileUtils deleteQuietly new File(filename)
        files remove filename
      }
    }

    files.get(filename) match {
      case Some(entry) if !entry.isDir =>
        delete(entry)
      case Some(_) =>
        fileSystem logMessage s"Cannot delete - path '$filename' is a directory!"
      case None =>
        fileSystem logMessage s"Cannot delete - path '$filename' does not exist!"
    }
  }

  private def allFiles(files: concurrent.Map[String, Entry]): Iterable[String] =
    files.map(_._1)

  @tailrec
  private def acquire(fileSystem: FileSystem, entry: Entry): Boolean = {
    val state = entry.state.get

    state match {
      case Idle =>
        val isSet = entry.state.compareAndSet(Idle, Copying(1))
        if (isSet) true else acquire(fileSystem, entry)
      case Copying(n) =>
        val isSet = entry.state.compareAndSet(state, Copying(n + 1))
        if (isSet) true else acquire(fileSystem, entry)
      case Creating | Deleting =>
        fileSystem logMessage "File inaccessible, cannot copy."
        false
    }
  }

  @tailrec
  private def release(fileSystem: FileSystem, entry: Entry): Boolean = {
    val state = entry.state.get

    state match {
      case Creating =>
        val isSet = entry.state.compareAndSet(Creating, Idle)
        if (isSet) true else release(fileSystem, entry)
      case Copying(1) =>
        val isSet = entry.state.compareAndSet(state, Idle)
        if (isSet) true else release(fileSystem, entry)
      case Copying(n) if n > 0 =>
        val isSet = entry.state.compareAndSet(state, Copying(n - 1))
        if (isSet) true else release(fileSystem, entry)
      case _: Copying =>
        fileSystem logMessage "Cannot have 0 or less copies in progress!"
        false
      case Idle =>
        fileSystem logMessage "Released more times than acquired."
        false
      case Deleting =>
        fileSystem logMessage "Releasing a file that is being deleted!"
        false
    }
  }

  @tailrec
  private def prepareForDelete(fileSystem: FileSystem, entry: Entry): Boolean = {
    val state = entry.state.get

    state match {
      case Idle =>
        val isSet = entry.state compareAndSet(Idle, Deleting)
        if (isSet) true else prepareForDelete(fileSystem, entry)
      case Creating =>
        fileSystem logMessage "File currently being created, cannot delete."
        false
      case _: Copying =>
        fileSystem logMessage "File currently being copied, cannot delete."
        false
      case Deleting =>
        false
    }
  }

}
