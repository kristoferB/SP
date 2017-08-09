//libraryDependencies += "com.qubit" % "akka-cloudpubsub_2.11" % "1.0.0"
//libraryDependencies += "com.google.guava" % "guava" % "20.0"



//för wabasabi
//resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/"

libraryDependencies ++= {
  val akkaV       = "2.4.17"
  val akkaStreamV = "2.0.5"
  val scalaTestV  = "2.2.5"

  Seq(
    "com.typesafe.akka" %% "akka-persistence" % "2.4.16",
    "org.iq80.leveldb"            % "leveldb"          % "0.7",
    "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
    "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    "com.google.cloud" % "google-cloud" % "0.19.0-alpha"
  )
}
