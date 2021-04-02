package org.learning.concurrency.ftp.server

import org.apache.commons.io.FileUtils

import java.io.File
import java.text.SimpleDateFormat

case class FileInfo(name: String,
                    path: String,
                    parent: String,
                    isDir: Boolean,
                    size: Long,
                    modified: String,
                    fileState: FileState)

object FileInfo {

  private val dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  def apply(file: File): FileInfo =
    new FileInfo(
      name = file.getName,
      path = file.getPath,
      parent = file.getParent,
      isDir = file.isDirectory,
      size = if (file.isDirectory) -1 else FileUtils.sizeOf(file),
      modified = dateFormatter format file.lastModified(),
      fileState = FileState.Idle)

  def create(file: File, size: Long): FileInfo =
    new FileInfo(
      name = file.getName,
      path = file.getPath,
      parent = file.getParent,
      isDir = file.isDirectory,
      size,
      modified = dateFormatter format file.lastModified(),
      fileState = FileState.Created)

}
