package org.learning.concurrency.ftp.server

sealed trait FileEvent

object FileEvent {

  case class Created(path: String) extends FileEvent

  case class Modified(path: String) extends FileEvent

  case class Deleted(path: String) extends FileEvent

}
