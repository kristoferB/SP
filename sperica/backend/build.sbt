import SPSettings._

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % versions.akka,
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "com.google.cloud" % "google-cloud" % "0.19.0-alpha"
)
