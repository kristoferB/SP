lazy val deps = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.8",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "com.lihaoyi" %% "upickle" % "0.4.3",
   "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.json4s" %% "json4s-native" % "3.4.0",
  "org.json4s" %% "json4s-ext" % "3.4.0"
)


lazy val commonSettings = packAutoSettings ++ Seq(
  scalaVersion := "2.11.8",
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/Releases",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"),
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

lazy val spservices = project.in( file(".") )
  //.aggregate(labkit, modelsTest)


lazy val labkit = project
  .dependsOn(spservices)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= deps)

lazy val modelsTest = project
  .dependsOn(spservices)
.settings(commonSettings: _*)
.settings(libraryDependencies ++= deps)
