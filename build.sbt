name := "SequencePlanner"
scalaVersion := "2.11.8"
version := "2.0_M1"

lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.8",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.7"
)

lazy val json = Seq(
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.json4s" %% "json4s-native" % "3.4.0",
  "org.json4s" %% "json4s-ext" % "3.4.0"
)

lazy val support = Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"
)

lazy val commonSettings = packSettings ++ Seq(
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
  ),
  packMain:= Map("SP"->"sp.launch.SP"),
  packResourceDir += (baseDirectory.value/ "../gui/web" -> "bin/gui/web")

)


lazy val root = project.in( file(".") )
  //.aggregate(spdomain, spcore, spservices, macros, gui)

lazy val spdomain = project
  .settings(commonSettings: _*)

lazy val spcore = project
  .dependsOn(spdomain, macros)
  .settings(commonSettings: _*)

lazy val spservices = project
  .dependsOn(spdomain)
  .settings(commonSettings: _*)

lazy val macros = project
  .settings(commonSettings: _*)
  .settings(libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _))
  .settings(libraryDependencies ++= json)

lazy val gui = project

lazy val sp1 = project


lazy val labkit = (project in file("spservices/labkit"))
  .dependsOn(spdomain, macros)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= akka)

lazy val modelsTest = (project in file("spservices/modelsTest"))
  .dependsOn(spdomain, macros)
.settings(commonSettings: _*)
.settings(libraryDependencies ++= akka)

lazy val opcua = (project in file("spservices/opcua"))
  .dependsOn(spdomain, macros)
.settings(commonSettings: _*)
.settings(libraryDependencies ++= akka)

// lazy val models = project.dependsOn(domain, extensions).
//   settings(commonSettings: _*).
//   settings(libraryDependencies ++= akka ++ json ++ support)

// lazy val macros = project.
//   settings(commonSettings: _*).
//   settings(libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)).
//   settings(libraryDependencies ++= json)


// lazy val core = project.dependsOn(domain).
//   settings(commonSettings: _*).
//   settings(libraryDependencies ++= akka ++ json)


// lazy val gui = project.dependsOn(domain, core).
//   settings(commonSettings: _*).
//   settings(libraryDependencies ++= akka ++ json)

// lazy val extensions = project.dependsOn(domain, core).
//   settings(commonSettings: _*).
//   settings(libraryDependencies ++= akka ++ json)


// lazy val launch = project.dependsOn(domain, core, gui, extensions).
//   settings(commonSettings: _*).
//   settings(libraryDependencies ++= akka ++ json)

// lazy val services = project.dependsOn(domain, extensions).
//   settings(commonSettings: _*).
//   settings(libraryDependencies ++= akka ++ json ++ support)
