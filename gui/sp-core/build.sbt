enablePlugins(ScalaJSPlugin)

name := "sp-scalajs-frontend"

version := "0.0.1"

scalaVersion := "2.11.8"

// create launcher file. Searches for something that extends JSApp
persistLauncher := true

//TODO
//val scalaJSReactVersion = "0.11.3"
//val scalaCssVersion = "0.5.1"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0",
  // the scalajs wrapper
  "com.github.japgolly.scalajs-react" %%% "core" % "0.11.3",
  "com.github.japgolly.scalajs-react" %%% "extra" % "0.11.3",
  "com.github.japgolly.scalacss" %%% "core" % "0.5.1",
  "com.github.japgolly.scalacss" %%% "ext-react" % "0.5.1",
  "fr.hmil" %%% "roshttp" % "2.0.0-RC1"
  //"org.webjars.npm" % "react-grid-layout" % "0.13.9"
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
    commonJSName "ReactDOMServer"
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

