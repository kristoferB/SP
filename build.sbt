name := "SequencePlanner"

lazy val default = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.0.0",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "org.json4s" %% "json4s-ext" % "3.2.11",
  "org.slf4j" % "slf4j-simple" % "1.7.7"
)

lazy val commonSettings = Seq(
  version := "0.5.0-SNAPSHOT",
  scalaVersion := "2.11.6",
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/Releases",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"),
  scalacOptions  := Seq(
    "-encoding", "utf8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-target:jvm-1.8",
    "-language:implicitConversions",
    "-language:postfixOps"
  )
)





lazy val root = project.in( file(".") )
   .aggregate(core, gui, extensions, launch)

lazy val core = project.
  settings(commonSettings: _*).
  settings(libraryDependencies ++= default)

lazy val gui = project.dependsOn(core).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= default)

lazy val extensions = project.dependsOn(core).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= default)

lazy val launch = project.dependsOn(core).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= default)



