name := "futures-and-promises"

organization := "org.learning.concurrency"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:postfixOps")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
  "org.scalaz" %% "scalaz-concurrent" % "7.2.30",
  "org.scala-lang.modules" %% "scala-async" % "0.10.0",
  "commons-io" % "commons-io" % "2.6")

connectInput in run := true

fork := true
