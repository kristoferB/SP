name := "SequencePlanner_core"
version := "2.0_M1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % "2.4.16",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "com.typesafe.akka" %% "akka-http-core" % "10.0.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.3",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1"
)
