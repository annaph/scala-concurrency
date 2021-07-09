name := "reactors"

organization := "org.learning.concurrency"

version := "1.0"

scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-feature",
  "-unchecked")

libraryDependencies ++= Seq(
  "io.reactors" %% "reactors" % "0.8")

fork := true
