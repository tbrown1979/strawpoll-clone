name := """straw-poll-app"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "net.debasishg"          %% "redisclient"  % "2.13",
  "com.github.nscala-time" %% "nscala-time"  % "1.4.0",
  "com.codahale.metrics"   %  "metrics-core" % "3.0.1"
)
