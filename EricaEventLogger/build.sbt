scalaOrganization in ThisBuild := "org.typelevel"
scalaVersion := "2.11.8"

resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/Releases",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.16",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.16",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.16",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.json4s" %% "json4s-native" % "3.4.0",
  "org.json4s" %% "json4s-ext" % "3.4.0",
  "org.json4s" %% "json4s-jackson"                              % "3.4.0",
  "com.github.nscala-time" %% "nscala-time"                     % "1.8.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1"
)
