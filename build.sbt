import SPSettings._


lazy val serviceSettings = Seq(
  fork := true,
  javaOptions += s"-Dconfig.file=${root.base.getCanonicalPath}/cluster.conf",
  connectInput in run := true
)

lazy val root = project.in( file(".") )
  //.aggregate(SPSettings.spdomain, SPSettings.spcomm, SPSettings.spcore, SPSettings.spgui)


lazy val spdomain = (crossProject.crossType(CrossType.Pure) in file("spdomain"))
  .settings(name := "spdomain")
  .settings(commonSettings)
  .settings(libraryDependencies ++= domainDependencies.value)
  .jvmSettings(
    libraryDependencies += "org.joda" % "joda-convert" % "1.8.2"
  )
  .jsSettings(
    jsSettings
  )

lazy val spdomainJVM = spdomain.jvm
lazy val spdomainJS = spdomain.js

lazy val spcomm = project
  .dependsOn(spdomainJVM)
  .settings(commonSettings)
  .settings(libraryDependencies ++= commDependencies.value)

lazy val spcore = project
  .dependsOn(spdomainJVM, spcomm)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= domainDependencies.value)
  .settings(libraryDependencies ++= commDependencies.value)


lazy val spgui = project
  .dependsOn(spdomainJS)
  .settings(commonSettings: _*)
  .settings(jsSettings: _*)
  .enablePlugins(ScalaJSPlugin)

lazy val sp1 = project

val backendDeps: Seq[ClasspathDep[ProjectReference]] = Seq(spdomainJVM, spcomm, spcore)
val frontendDeps: Seq[ClasspathDep[ProjectReference]] = Seq(spdomainJS, spgui)


//lazy val spcontrol = crossProject.in(file("spcontrol"))
//    .dependsOn(spdomain)
//    .settings(
//      libraryDependencies ++= domainDependencies.value,
//      commonSettings
//      )
//    .jvmSettings(
//      libraryDependencies ++= Seq(
//        "com.typesafe.akka" %% "akka-persistence" % versions.akka,
//        "org.iq80.leveldb"            % "leveldb"          % "0.7",
//        "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
//        "com.github.nscala-time" %% "nscala-time" % "2.16.0"
//      ),
//      libraryDependencies ++= commDependencies.value,
//      serviceSettings
//    )
//    .jsSettings(
//      jsSettings
//    )
//
//lazy val spcontrolJVM = spcontrol.jvm.dependsOn(backendDeps:_*)
//lazy val spcontrolJS = spcontrol.js.dependsOn(frontendDeps:_*)


lazy val spcontrolAPI = crossProject.crossType(CrossType.Pure).in(file("spcontrol/api"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings
  )
  .dependsOn(spdomain)

lazy val spcontrolAPIJVM = spcontrolAPI.jvm
lazy val spcontrolAPIJS = spcontrolAPI.js

lazy val spcontrolBackend = project.in(file("spcontrol/backend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    libraryDependencies ++= commDependencies.value,
    commonSettings,
    serviceSettings
  )
  .dependsOn(spdomainJVM, spcontrolAPIJVM, spcomm)

lazy val spcontrolFrontEnd = project.in(file("spcontrol/frontend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings,
    jsSettings
  )
  .dependsOn(spdomainJS, spcontrolAPIJS, spgui)
  .enablePlugins(ScalaJSPlugin)
