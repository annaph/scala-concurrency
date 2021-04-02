package org.learning.concurrency.ftp.client

import org.learning.concurrency.ftp.Implicits._
import org.learning.concurrency.ftp.server.FileInfo
import org.learning.concurrency.ftp.{ScalaFtpObservableTransformers, swing}

import java.io.File
import javax.swing.table.DefaultTableModel
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Dialog
import scala.swing.event.ButtonClicked
import scala.util.{Failure, Success}

trait FtpClientLogic {
  self: FtpClientFrame with FtpClientApi =>

  setupPane(FilesGridPanel.LeftPane)
  setupPane(FilesGridPanel.RightPane)

  AppMenuBar.FileMenu.ExitMenuItem.reactions += {
    case ButtonClicked(_) =>
      stopClientApi().onComplete {
        case Success(_) =>
          sys exit 0
        case Failure(e) =>
          updateStatusPanel(text = s"Could not stop client API: $e")
      }
  }

  AppMenuBar.HelpMenu.AboutMenuItem.reactions += {
    case ButtonClicked(_) =>
      Dialog.showMessage(
        title = "About ScalaFTP",
        message = "ScalaFTP version 1.0, made in Ireland")
  }

  connected.onComplete {
    case Success(true) =>
      swing {
        updateStatusPanel(text = "Connected.", refreshFilePanes = true)
      }
    case Success(false) =>
      swing {
        updateStatusPanel(text = "Could not find server!")
      }
    case Failure(e) =>
      swing {
        updateStatusPanel(text = s"Could not connect to server: $e")
      }
  }

  private def setupPane(filePane: FilePane): Unit = {
    filePane.table
      .rowDoubleClicks
      .map(filePane.dirFiles(_))
      .filter(_.isDir)
      .subscribeObserver(tableObserver(filePane)(_))

    filePane.PathBar.UpButton
      .clicks
      .subscribeObserver(upButtonObserver(filePane)(_))

    filePane.ButtonsGridPanel.CopyButton
      .clicks
      .compose(ScalaFtpObservableTransformers.toFileInfo(filePane))
      .map(fileInfo =>
        fileInfo -> FilesGridPanel.oppositeFilePane(filePane).currentPath)
      .subscribeObserver(copyButtonObserver)

    filePane.ButtonsGridPanel.DeleteButton
      .clicks
      .compose(ScalaFtpObservableTransformers.toFileInfo(filePane))
      .subscribeObserver(deleteButtonObserver)

    filePane.ButtonsGridPanel.RefreshButton
      .clicks
      .subscribeObserver { _ =>
        val oppositeFilePane = FilesGridPanel oppositeFilePane filePane
        refreshFilePane(filePane, filePane.currentPath)
        refreshFilePane(oppositeFilePane, oppositeFilePane.currentPath)
      }

  }

  private def tableObserver(filePane: FilePane): FileInfo => Unit = fileInfo => {
    val path = filePane.currentPath + File.separator + fileInfo.name
    refreshFilePane(filePane, path)
  }

  private def upButtonObserver(filePane: FilePane): Unit => Unit = _ => {
    val path = filePane.parent
    refreshFilePane(filePane, path)
  }

  private def copyButtonObserver: ((FileInfo, String)) => Unit = {
    case (fileInfo, destDir) =>
      val srcPath = fileInfo.path
      val destPath = destDir + File.separator + fileInfo.name

      copyFile(srcPath, destPath).onComplete {
        case Success(srcPath) =>
          updateStatusPanel(text = s"File copied: $srcPath", refreshFilePanes = true)
        case Failure(e) =>
          updateStatusPanel(text = s"Could not copy file: $e")
      }
  }

  private def deleteButtonObserver: FileInfo => Unit = fileInfo => {
    deleteFile(fileInfo.path).onComplete {
      case Success(srcPath) =>
        updateStatusPanel(text = s"File deleted: $srcPath", refreshFilePanes = true)
      case Failure(e) =>
        updateStatusPanel(text = s"Could not delete file: $e")
    }
  }

  private def refreshFilePane(filePane: FilePane, path: String): Unit =
    fileList(path).onComplete {
      case Success((dir, files)) =>
        swing {
          updateFilePane(filePane, dir, files)
        }
      case Failure(e) =>
        swing {
          updateStatusPanel(text = s"Could not update file pane: $e")
        }
    }

  private def updateFilePane(filePane: FilePane, dir: String, files: Seq[FileInfo]): Unit = {
    filePane.parent = if (dir == ".") dir else dir take dir.lastIndexOf(File.separator)

    filePane.dirFiles = {
      val (dirs, nonDirs) = files.partition(_.isDir)
      dirs.sortBy(_.name) concat nonDirs.sortBy(_.name)
    }

    filePane.PathBar.PathTextField.text = dir

    filePane.table.model match {
      case model: DefaultTableModel =>
        model setRowCount 0
        for (file <- filePane.dirFiles) model addRow file
    }
  }

  private def updateStatusPanel(text: String, refreshFilePanes: Boolean = false): Unit = {
    StatusPanel.StatusLabel.text = text

    if (refreshFilePanes) {
      refreshFilePane(
        filePane = FilesGridPanel.LeftPane,
        path = FilesGridPanel.LeftPane.currentPath)

      refreshFilePane(
        filePane = FilesGridPanel.RightPane,
        path = FilesGridPanel.RightPane.currentPath)
    }
  }

}
