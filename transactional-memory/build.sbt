name := "transactional-memory"

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
  "org.scala-stm" %% "scala-stm" % "0.9.1")

fork := true
