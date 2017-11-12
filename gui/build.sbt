enablePlugins(ScalaJSPlugin)

name := "spgui"

version := "0.0.1"

scalaOrganization in ThisBuild := "org.typelevel"

scalaVersion := "2.11.8"

scalacOptions  := Seq(
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:implicitConversions",
  "-language:postfixOps"
)

val scalaJSReactVersion = "1.0.0"
val scalaCssVersion = "0.5.3-RC1"
val diodeVersion = "1.1.2"


libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,
  "io.suzaku" %%% "diode" % diodeVersion,
  "io.suzaku" %%% "diode-react" % diodeVersion,
  "com.lihaoyi" %%% "upickle" % "0.4.3",
  "com.lihaoyi" %%% "scalarx" % "0.3.2",
  "org.singlespaced" %%% "scalajs-d3" % "0.3.3",
  "org.scalatest" %%% "scalatest" % "3.0.0" % "test",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "eu.unicredit" %%% "paths-scala-js" % "0.4.5"
)

// This is how to include js files. Put it in src/main/resources.
jsDependencies ++= Seq(
  ProvidedJS / "ganttApp.js"
)
