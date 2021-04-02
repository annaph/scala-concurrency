package org.learning.concurrency.ftp.client

import javax.swing.UIManager
import scala.swing.{Frame, SimpleSwingApplication}
import scala.util.Try

object FtpClientApp extends SimpleSwingApplication {

  var serverHostArg: String = _

  Try(UIManager setLookAndFeel UIManager.getSystemLookAndFeelClassName).recover {
    case e =>
      println(s"Could not change look & feel: $e")
  }

  override def main(args: Array[String]): Unit = {
    serverHostArg = if (args.length > 0) args(0) else {
      println("No server host argument provided!")
      sys exit 1
    }

    super.main(args)
  }

  override def top: Frame = new FtpClientFrame with FtpClientApi with FtpClientLogic {
    override def serverHost: String = serverHostArg
  }

}
