package org.learning.concurrency.reactive.extensions

import io.reactivex.rxjava3.schedulers.Schedulers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.swing.BorderPanel.Position
import scala.swing.event.ButtonClicked
import scala.swing.{BorderPanel, Button, Dimension, Frame, Label, MainFrame, SimpleSwingApplication, TextArea, TextField}

object SchedulersComputation extends App {

  val scheduler = Schedulers.computation()
  val numbers = Observable.from(0 until 20)

  numbers.subscribe(number => log(s"number $number"))
  numbers.observeOn(scheduler).subscribe(number => log(s"number $number"))

  Thread sleep 3000

}

object SchedulersSwing extends SimpleSwingApplication {

  override def top: Frame = new MainFrame {
    title = "Swing Observables"

    val button: Button = new Button {
      text = "Click"
    }

    contents = button

    val buttonClicks: Observable[Unit] = Observable.create { emitter =>
      button.reactions += {
        case ButtonClicked(_) =>
          emitter onNext()
      }
    }

    buttonClicks.subscribe(_ => log("button clicked"))
  }

}

object SchedulersBrowser extends SimpleSwingApplication {

  override def top: Frame = new BrowserFrame with BrowserLogic

  trait BrowserLogic {
    self: BrowserFrame =>

    button.clicks
      .map(_ => pageRequest(urlField.text))
      .concat
      .observeOn(swingScheduler)
      .subscribe { response =>
        log("Button clicked")
        pageArea.text = response
      }

    urlField.texts.map(suggestRequest)
      .concat
      .observeOn(swingScheduler)
      .subscribe { response =>
        log("URL field text changed")
        pageArea.text = response
      }

    def pageRequest(url: String): Observable[String] = {
      val request = Future {
        val file = Source fromURL url
        try file.mkString finally file.close()
      }

      Observable.from(request)
        .timeout(4.seconds)
        .onErrorResumeNext(e => Observable items s"Could not load page: $e")
    }

    def suggestRequest(term: String): Observable[String] = {
      val request = Future {
        val url = s"http://suggestqueries.google.com/complete/search?client=firefox&q=$term"
        val file = Source fromURL url
        try file.mkString finally file.close()
      }

      Observable.from(request)
        .timeout(0.5.seconds)
        .onErrorResumeNext(e => Observable items s"(no suggestions): $e")
    }

  }

  class BrowserFrame extends MainFrame {
    title = "MiniBrowser"

    val urlLabel = new Label("URL:")
    val urlField = new TextField("http://www.w3.org/Addressing/URL/url-spec.txt")
    val button: Button = new Button {
      text = "Feeling Lucky"
    }
    val pageArea = new TextArea

    contents = new BorderPanel {
      layout(new BorderPanel {
        layout(urlLabel) = Position.West
        layout(urlField) = Position.Center
        layout(button) = Position.East
      }) = Position.North

      layout(pageArea) = Position.Center
    }

    size = new Dimension(1024, 768)
  }

}
