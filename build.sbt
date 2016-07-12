name := "SequencePlanner"
scalaVersion := "2.11.8"

lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.8",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.7"
)

lazy val json = Seq(
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.json4s" %% "json4s-native" % "3.4.0",
  "org.json4s" %% "json4s-ext" % "3.4.0"
)

lazy val support = Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
)

lazy val commonSettings = Seq(
  version := "0.6.0-SNAPSHOT",
  scalaVersion := "2.11.7",
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
   .aggregate(core, domain, gui, extensions, launch)

lazy val domain = project.
  settings(commonSettings: _*).
  settings(libraryDependencies ++= json ++ support)

lazy val core = project.dependsOn(domain).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= akka ++ json)

lazy val gui = project.dependsOn(domain, core).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= akka ++ json)

lazy val extensions = project.dependsOn(domain, core).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= akka ++ json)

lazy val launch = project.dependsOn(domain, core, gui, extensions).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= akka ++ json)



