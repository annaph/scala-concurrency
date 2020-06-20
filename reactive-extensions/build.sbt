name := "reactive-extensions"

organization := "org.learning.concurrency"

version := "1.0"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.1.1",
  "io.reactivex.rxjava3" % "rxjava" % "3.0.2",
  "commons-io" % "commons-io" % "2.6")

fork := true
