enablePlugins(ScalaJSPlugin)

name := "sp-scalajs-frontend"

version := "0.0.1"

scalaVersion := "2.11.8"

// create launcher file. Searches for something that extends JSApp
persistLauncher := true

val scalaJSReactVersion = "0.11.3"
val scalaCssVersion = "0.5.1"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.5.0",
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,

  "fr.hmil" %%% "roshttp" % "2.0.0-RC1"
)

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
    commonJSName "ReactDOMServer",

  "org.webjars.bower" % "react-grid-layout" % "0.13.5"
    /        "react-grid-layout.min.js"
    minified "react-grid-layout.min.js"
    commonJSName "ReactGridLayout"
)

// copy javascript files to js folder that are generated using fastOptJS/fullOptJS
val targetDirectory = file("build")
crossTarget  in (Compile, fullOptJS)                     := targetDirectory
crossTarget  in (Compile, fastOptJS)                     := targetDirectory
crossTarget  in (Compile, packageJSDependencies)         := targetDirectory
crossTarget  in (Compile, packageScalaJSLauncher)        := targetDirectory
crossTarget  in (Compile, packageMinifiedJSDependencies) := targetDirectory
artifactPath in (Compile, fastOptJS)                     :=
((crossTarget in (Compile, fastOptJS)).value / ((moduleName in fastOptJS).value + "-opt.js"))

