name := "SequencePlanner_1"
scalaVersion := "2.11.8"
version := "1.0.0"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.8",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.8",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.codemettle.reactivemq" %% "reactivemq" % "0.5.5",
  "org.apache.activemq" % "activemq-client"   % "5.13.3",
  "de.ummels" %% "scala-prioritymap" % "0.5.0",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-testkit" % "1.3.3",
  "io.spray" %%  "spray-json" % "1.3.2",
  "io.netty" % "netty-all" % "4.1.6.Final",
  "com.codepoetics" % "protonpack" % "1.1",
  "org.jooq" % "jool" % "0.9.12",
  "io.netty" % "netty-handler" % "4.1.6.Final",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "com.google.guava" % "guava" % "20.0",
  "com.google.code.findbugs" % "jsr305" % "3.0.1",
  "org.eclipse.milo" % "sdk-client" % "0.1.0",
  "org.scala-lang.modules" % "scala-java8-compat_2.11" % "0.3.0",
"com.github.nscala-time" %% "nscala-time" % "2.12.0",
"org.json4s" %% "json4s-native" % "3.4.0",
"org.json4s" %% "json4s-ext" % "3.4.0",
"org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
"org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.lihaoyi" %% "upickle" % "0.4.3"
)


packSettings

  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/Releases",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "spray repo" at "http://repo.spray.io"
  )

  scalacOptions  := Seq(
    "-encoding", "utf8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-target:jvm-1.8",
    "-language:implicitConversions",
    "-language:postfixOps"
  )

  packMain:= Map("SP"->"sp.launch.SP")

  packResourceDir += (baseDirectory.value/ "../gui/web" -> "bin/gui/web")





