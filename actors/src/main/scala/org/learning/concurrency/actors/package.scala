package org.learning.concurrency

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}

package object actors {

  lazy val ourSystem: ActorSystem = ActorSystem("OurExampleSystem")

  def remotingSystem(name: String, port: Int): ActorSystem =
    ActorSystem(name, remotingConfig(port))

  private def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
       |akka {
       |  actor {
       |    provider = remote
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

  def log(msg: String): Unit = {
    println(s"${Thread.currentThread.getName}: $msg")
  }

}
