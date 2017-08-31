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


lazy val spControlAPI = crossProject.crossType(CrossType.Pure).in(file("spcontrol/api"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings
  )
  .dependsOn(spdomain)

lazy val spControlAPIJVM = spControlAPI.jvm
lazy val spControlAPIJS = spControlAPI.js

lazy val spControlBackend = project.in(file("spcontrol/backend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    libraryDependencies ++= commDependencies.value,
    commonSettings,
    serviceSettings
  )
  .dependsOn(spControlAPIJVM)
  .dependsOn(backendDeps:_*)


lazy val spControlFrontEnd = project.in(file("spcontrol/frontend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings,
    jsSettings
  )
  .dependsOn(spControlAPIJS)
  .dependsOn(frontendDeps:_*)
  .enablePlugins(ScalaJSPlugin)
