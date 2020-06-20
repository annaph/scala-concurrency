package org.learning.concurrency.reactive

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.swing.SwingUtilities

import scala.swing.event.{ButtonClicked, ValueChanged}
import scala.swing.{Button, TextField}

package object extensions {

  val swingScheduler: Scheduler = Schedulers.from {
    command => SwingUtilities invokeLater command
  }

  def log(msg: String): Unit = {
    println(s"${Thread.currentThread.getName}: $msg")
  }

  implicit class ButtonOps(self: Button) {
    def clicks: Observable[Unit] = Observable.create { emitter =>
      self.reactions += {
        case ButtonClicked(_) =>
          emitter onNext()
      }
    }
  }

  implicit class TextFieldOps(self: TextField) {
    def texts: Observable[String] = Observable.create { emitter =>
      self.reactions += {
        case ValueChanged(_) =>
          emitter onNext self.text
      }
    }
  }

}
