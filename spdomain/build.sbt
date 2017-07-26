name := "SequencePlanner_domain"
scalaVersion := "2.11.8"
version := "2.0_M1"



libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
    "com.typesafe.play" %% "play-json" % "2.6.0",
    "org.julienrf" %% "play-json-derived-codecs" % "4.0.0",
    "io.github.cquiroz" %% "scala-java-time" % "2.0.0-M12",
  "joda-time" % "joda-time" % "2.9.9" // to be removed soon. Not dep in JS!
)