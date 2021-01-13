name := "actors"

organization := "org.learning.concurrency"

version := "1.0"

scalaVersion := "2.13.3"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-feature",
  "-unchecked")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.9",
  "com.typesafe.akka" %% "akka-remote" % "2.6.9",
  "io.aeron" % "aeron-driver" % "1.27.0",
  "io.aeron" % "aeron-client" % "1.27.0",
  "commons-io" % "commons-io" % "2.8.0")

fork := true
