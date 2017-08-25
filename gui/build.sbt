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

val scalaJSReactVersion = "1.1.0"
val scalaCssVersion = "0.5.3-RC1"
val diodeVersion = "1.1.2"

lazy val scalajsGoogleChartsVersion = "0.6.0.Alpha"
lazy val chartsScalaVersion = "2.11"
lazy val chartsSbtVersion = "0.13"
lazy val aleastchsBintray = "https://dl.bintray.com/aleastchs/aleastChs-releases/org.aleastChs/"
lazy val charts ="scalajs-google-charts"
lazy val googleChartsUrl = aleastchsBintray +
  charts +"/scala_"+
  chartsScalaVersion +"/sbt_"+ chartsSbtVersion +"/"+
  scalajsGoogleChartsVersion +"/jars/"+ charts +".jar"

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
  "eu.unicredit" %%% "paths-scala-js" % "0.4.5",
  "org.aleastChs" % charts % scalajsGoogleChartsVersion
)

/* This is how to include js files. Put it in src/main/resources.
jsDependencies ++= Seq(
  ProvidedJS / "SomeJSFile.js"
)
*/