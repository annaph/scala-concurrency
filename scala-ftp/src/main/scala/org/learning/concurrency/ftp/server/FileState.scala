package org.learning.concurrency.ftp.server

sealed trait FileState {

  def inc: FileState

  def dec: FileState

}

object FileState {

  case class Copying(n: Int) extends FileState {

    override def inc: FileState =
      Copying(n + 1)

    override def dec: FileState =
      if (n > 1) Copying(n - 1) else Idle

  }

  case object Created extends FileState {

    override def inc: FileState =
      throw new UnsupportedOperationException("Cannot copy created.")

    override def dec: FileState =
      throw new UnsupportedOperationException("Created not copied.")

  }

  case object Idle extends FileState {

    override def inc: FileState =
      Copying(1)

    override def dec: FileState =
      throw new UnsupportedOperationException("Idle not copied.")

  }

  case object Deleted extends FileState {

    override def inc: FileState =
      throw new UnsupportedOperationException("Cannot copy deleted.")

    override def dec: FileState =
      throw new UnsupportedOperationException("Deleted not copied.")

  }

}
