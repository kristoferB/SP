enablePlugins(ScalaJSPlugin)

name := "sp--example"

version := "0.0.1"

scalaVersion := "2.11.8"

val scalaJSReactVersion = "0.11.3"

val scalaCssVersion = "0.5.1"

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % "0.11.3",
  "com.github.japgolly.scalajs-react" %%% "extra" % "0.11.3",
  "com.github.japgolly.scalacss" %%% "core" % "0.5.1",
  "com.github.japgolly.scalacss" %%% "ext-react" % "0.5.1")

jsDependencies ++= Seq(
  "org.webjars.bower" % "react" % "15.3.2"
    /         "react-dom.js"
    minified  "react-dom.min.js"
    dependsOn "react-with-addons.js"
    commonJSName "ReactDOM",

  "org.webjars.bower" % "react" % "15.3.2"
    /        "react-with-addons.js"
    minified "react-with-addons.min.js"
    commonJSName "React",

  "org.webjars.bower" % "react" % "15.3.2"
    /         "react-dom-server.js"
    minified  "react-dom-server.min.js"
    dependsOn "react-dom.js"
    commonJSName "ReactDOMServer"
)

//copy javascript files to js folder that are generated using fastOptJS/fullOptJS
crossTarget in (Compile, fullOptJS) := file("build")
crossTarget in (Compile, fastOptJS) := file("build")
crossTarget in (Compile, packageScalaJSLauncher) := file("build")

artifactPath in (Compile, fastOptJS) := ((crossTarget in (Compile, fastOptJS)).value /
  ((moduleName in fastOptJS).value + "-widget.js"))


