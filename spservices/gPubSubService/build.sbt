libraryDependencies += "com.qubit" % "akka-cloudpubsub_2.11" % "1.0.0"
libraryDependencies += "com.google.guava" % "guava" % "20.0"

//för wabasabi
resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/"

libraryDependencies ++= {
  val akkaV       = "2.4.17"
  val akkaStreamV = "2.0.5"
  val scalaTestV  = "2.2.5"

  Seq(
    "com.github.gphat" % "wabisabi_2.11" % "2.2.0",
    "com.typesafe.akka" %% "akka-persistence" % "2.4.16",
    "org.iq80.leveldb"            % "leveldb"          % "0.7",
    "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
    "com.github.nscala-time" %% "nscala-time" % "2.12.0",
    "org.json4s" %% "json4s-native" % "3.4.0",
    "org.json4s" %% "json4s-ext" % "3.4.0",
    "org.json4s" %% "json4s-jackson"                              % "3.4.0",
    "com.github.nscala-time" %% "nscala-time"                     % "1.8.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
    "com.typesafe.akka" %% "akka-http-core" % "10.0.3",
    "com.typesafe.akka" %% "akka-http" % "10.0.3",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.0.3",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1"
  )
}
