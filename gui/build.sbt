enablePlugins(ScalaJSPlugin)

name := "spgui"

version := "0.0.1"

scalaOrganization in ThisBuild := "org.typelevel"

scalaVersion := "2.11.8"

val scalaJSReactVersion = "0.11.3"
val scalaCssVersion = "0.5.1"
val diodeVersion = "1.1.0"


libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,
  "me.chrons" %%% "diode" % diodeVersion,
  "me.chrons" %%% "diode-react" % diodeVersion,
  "com.lihaoyi" %%% "upickle" % "0.4.3",
  "com.lihaoyi" %%% "scalarx" % "0.3.2",
  "com.softwaremill.quicklens" %%% "quicklens" % "1.4.8",
//  "com.zoepepper" %%% "scalajs-jsjoda" % "1.0.4",   // probably good to use this when we need time
//  "com.zoepepper" %%% "scalajs-jsjoda-as-java-time" % "1.0.4",
  "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
)

/* This is how to include js files. Put it in src/main/resources.
jsDependencies ++= Seq(
  ProvidedJS / "SomeJSFile.js"
)
*/
