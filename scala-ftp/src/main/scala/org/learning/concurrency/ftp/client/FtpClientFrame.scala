package org.learning.concurrency.ftp.client

import org.learning.concurrency.ftp.server.FileInfo

import javax.swing.table.DefaultTableModel
import scala.swing.BorderPanel.Position._
import scala.swing._

trait FtpClientFrame extends MainFrame {

  title = "ScalaFTP"

  contents = new BorderPanel {
    layout(AppMenuBar) = North
    layout(FilesGridPanel) = Center
    layout(StatusPanel) = South
  }

}

object AppMenuBar extends MenuBar {

  object FileMenu extends Menu("File") {

    object ExitMenuItem extends MenuItem("Exit")

    contents += ExitMenuItem

  }

  object HelpMenu extends Menu("Help") {

    object AboutMenuItem extends MenuItem("About...")

    contents += AboutMenuItem

  }

  contents += FileMenu
  contents += HelpMenu

}

object FilesGridPanel extends GridPanel(1, 2) {

  def oppositeFilePane(filePane: FilePane): FilePane =
    if (filePane eq LeftPane) RightPane else LeftPane

  object LeftPane extends FilePane

  contents += LeftPane
  contents += RightPane

  object RightPane extends FilePane

}

object StatusPanel extends BorderPanel {

  object StaticLabel extends Label("Status: ")

  object StatusLabel extends Label("connecting...", null, Alignment.Left)

  layout(StaticLabel) = West
  layout(StatusLabel) = Center

}

class FilePane extends BorderPanel {

  var parent: String = "."

  var dirFiles: Seq[FileInfo] = Nil

  def currentPath: String =
    PathBar.PathTextField.text

  def table: Table =
    FileScrollPane.FileTable

  layout(PathBar) = North
  layout(FileScrollPane) = Center
  layout(ButtonsGridPanel) = South

  object PathBar extends BorderPanel {

    object PathLabel extends Label("Path:")

    object PathTextField extends TextField(".") {
      editable = false
    }

    object UpButton extends Button("^")

    layout(PathLabel) = West
    layout(PathTextField) = Center
    layout(UpButton) = East

  }

  object FileScrollPane extends ScrollPane {
    private val columnNames: Array[AnyRef] = Array("Filename", "Size", "Date modified")

    object FileTable extends Table {
      showGrid = true
      model = new DefaultTableModel(columnNames, 0) {
        override def isCellEditable(row: Int, column: Int): Boolean = false
      }
      selection.intervalMode = Table.IntervalMode.Single
    }

    contents = FileTable

  }

  object ButtonsGridPanel extends GridPanel(1, 2) {

    object CopyButton extends Button("Copy")

    object DeleteButton extends Button("Delete")

    object RefreshButton extends Button("Refresh")

    contents += CopyButton
    contents += DeleteButton
    contents += RefreshButton

  }

}
