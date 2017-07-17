name := "SequencePlanner_domain"
scalaVersion := "2.11.8"
version := "2.0_M1"

scalacOptions  := Seq(
    "-encoding", "utf8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-target:jvm-1.8",
    "-language:implicitConversions",
    "-language:postfixOps"
  )

  libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.json4s" %% "json4s-native" % "3.4.0",
  "org.json4s" %% "json4s-ext" % "3.4.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "com.lihaoyi" %% "upickle" % "0.4.3"
)
