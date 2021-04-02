package org.learning.concurrency

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import io.reactivex.rxjava3.core.{Observable, ObservableTransformer}
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import org.learning.concurrency.ftp.client.FilePane
import org.learning.concurrency.ftp.server.FileInfo

import java.awt.event.{KeyAdapter, KeyEvent, MouseAdapter, MouseEvent}
import javax.swing.SwingUtilities
import scala.swing.event.ButtonClicked
import scala.swing.{Button, Table}

package object ftp {

  def log(msg: String): Unit =
    println(s"${Thread.currentThread().getName}: $msg")

  def swing(body: => Unit): Unit = {
    val runnable = new Runnable {
      override def run(): Unit = body
    }

    SwingUtilities invokeLater runnable
  }

  def remotingSystem(name: String, port: Int): ActorSystem =
    ActorSystem(name, remotingConfig(port))

  private def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
       |akka {
       |  actor {
       |    provider = remote
       |    allow-java-serialization = on
       |    warn-about-java-serializer-usage = off
       |  }
       |  remote {
       |    artery {
       |      transport = tcp
       |      canonical.hostname = "127.0.0.1"
       |      canonical.port = $port
       |    }
       |  }
       |}
       |""".stripMargin)

  object Implicits {

    implicit def toRow(fileInfo: FileInfo): Array[AnyRef] = Array(
      fileInfo.name,
      if (fileInfo.isDir) "" else f"${fileInfo.size.toDouble / 1000}%1.2f kB",
      fileInfo.modified)

    implicit class ButtonOps(button: Button) {

      def clicks: Observable[Unit] = Observable.create { emitter =>
        button.reactions += {
          case ButtonClicked(_) =>
            emitter onNext()
        }
      }

    }

    implicit class TableOps(table: Table) {

      def rowDoubleClicks: Observable[Int] = Observable.create { emitter =>
        table.peer.addMouseListener(new MouseAdapter {
          override def mouseClicked(event: MouseEvent): Unit =
            if (event.getClickCount == 2) emitter onNext table.peer.getSelectedRow
        })

        table.peer.addKeyListener(new KeyAdapter {
          override def keyPressed(event: KeyEvent): Unit = {
            if (event.getKeyCode == KeyEvent.VK_ENTER) emitter onNext table.peer.getSelectedRow
          }
        })
      }

    }

    implicit class ObservableOps[T](observable: Observable[T]) {

      def subscribeObserver(onNext: T => Unit): Disposable = {
        val consumer: Consumer[T] = t => onNext(t)
        observable subscribe consumer
      }

    }

  }

  object ScalaFtpObservableTransformers {

    def toFileInfo(filePane: FilePane): ObservableTransformer[Unit, FileInfo] = upstream =>
      upstream
        .map(_ => filePane.table.peer.getSelectedRow)
        .filter(_ != -1)
        .map(filePane.dirFiles(_))

  }

}
