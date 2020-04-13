name := "parallel-collections"

organization := "org.learning.concurrency"

version := "1.0.0"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
  "com.quantifind" % "wisp_2.11" % "0.0.4")

fork := true
