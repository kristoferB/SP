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
  "org.singlespaced" %%% "scalajs-d3" % "0.3.3",
  "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
)

/* This is how to include js files. Put it in src/main/resources.
jsDependencies ++= Seq(
  ProvidedJS / "SomeJSFile.js"
)
*/
