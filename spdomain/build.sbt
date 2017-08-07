name := "SequencePlanner_domain"
version := "2.1"



libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "com.typesafe.play" %% "play-json" % "2.6.0",
  "org.julienrf" %% "play-json-derived-codecs" % "4.0.0",
  "io.github.cquiroz" %% "scala-java-time" % "2.0.0-M12",
  "org.joda" % "joda-convert" % "1.8.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.7.0"
)