name := "scala-ftp"

organization := "org.learning.concurrency"

version := "1.0"

scalaVersion := "2.13.3"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  "com.typesafe.akka" %% "akka-actor" % "2.6.12",
  "com.typesafe.akka" %% "akka-remote" % "2.6.12",
  "io.reactivex.rxjava3" % "rxjava" % "3.0.11",
  "org.scala-stm" %% "scala-stm" % "0.11.0",
  "com.storm-enroute" %% "scalameter-core" % "0.21",
  "commons-io" % "commons-io" % "2.8.0")

fork := true
