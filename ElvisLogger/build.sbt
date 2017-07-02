name := """Elvis-log"""

version := "2.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.1" % "test",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.1",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.codemettle.reactivemq" %% "reactivemq" % "0.5.0",
  "org.apache.activemq" % "activemq-client" % "5.13.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test")

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "org.json4s" %% "json4s-ext" % "3.3.0",
  "org.json4s" %% "json4s-jackson" % "3.3.0")

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "1.8.0"

organization := "sekvensa.elvis"
