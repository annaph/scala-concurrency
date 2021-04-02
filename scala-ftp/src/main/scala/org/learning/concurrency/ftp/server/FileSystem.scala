package org.learning.concurrency.ftp.server

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter

import java.io.File
import scala.concurrent.stm.{TMap, Txn, atomic}
import scala.jdk.CollectionConverters._

class FileSystem(val rootPath: String) {

  private val files = TMap[String, FileInfo]()

  def init(): Unit = atomic { implicit txn =>
    files.clear()

    val allFiles = FileUtils.iterateFilesAndDirs(new File(rootPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE).asScala

    allFiles.map(FileInfo(_)).foreach(fileInfo =>
      files(fileInfo.path) = fileInfo)
  }

  def addOrUpdateFileInfo(fileInfo: FileInfo): Unit =
    files.single.put(fileInfo.path, fileInfo)

  def removeFileInfo(fileInfoPath: String): Unit =
    files.single remove fileInfoPath

  def fileList(dir: String): Map[String, FileInfo] = atomic { implicit txn =>
    files.filter(_._2.parent == dir).toMap
  }

  def copyFile(srcPath: String, destPath: String): String = atomic { implicit txn =>
    if (!files.contains(srcPath)) throw new Exception(s"Source $srcPath does not exist.")

    if (files contains destPath) throw new Exception(s"Destination $destPath already exists.")

    val srcFileInfo = files(srcPath)

    if (srcFileInfo.isDir) throw new Exception(s"Source $srcPath is a directory.")

    srcFileInfo.fileState match {
      case FileState.Created =>
        throw new Exception(s"File $srcPath being created.")
      case FileState.Deleted =>
        throw new Exception(s"File $srcPath already deleted.")
      case FileState.Idle | FileState.Copying(_) =>
        val srcFile = new File(srcPath)
        val destFile = new File(destPath)

        files(srcPath) = srcFileInfo.copy(fileState = srcFileInfo.fileState.inc)
        files(destPath) = FileInfo.create(destFile, srcFileInfo.size)

        Txn.afterCommit(_ => copyOnDisk(srcFile, destFile))
        srcPath
    }
  }

  private def copyOnDisk(srcFile: File, destFile: File): Unit = {
    FileUtils.copyFile(srcFile, destFile)

    atomic { implicit txn =>
      val srcFileInfo = files(srcFile.getPath)
      val destFileInfo = files(destFile.getPath)

      files(srcFileInfo.path) = srcFileInfo.copy(fileState = srcFileInfo.fileState.dec)
      files(destFileInfo.path) = FileInfo(destFile)
    }
  }

  def deleteFile(srcPath: String): String = atomic { implicit txn =>
    if (!files.contains(srcPath)) throw new Exception(s"Source $srcPath does not exist.")

    val fileInfo = files(srcPath)

    fileInfo.fileState match {
      case FileState.Created =>
        throw new Exception(s"File $srcPath not yet created.")
      case FileState.Copying(_) =>
        throw new Exception(s"Cannot delete $srcPath, file being copied.")
      case FileState.Deleted =>
        throw new Exception(s"File $srcPath already being deleted.")
      case FileState.Idle =>
        files(srcPath) = fileInfo.copy(fileState = FileState.Deleted)
        Txn.afterCommit(_ => deleteFromDisk(new File(srcPath)))
        srcPath
    }
  }

  private def deleteFromDisk(srcFile: File): Unit = {
    FileUtils forceDelete srcFile
    files.single.remove(srcFile.getPath)
  }

}

object FileSystem {

  def apply(rootPath: String): FileSystem =
    new FileSystem(rootPath)

}
