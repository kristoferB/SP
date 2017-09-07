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

lazy val spdomain_jvm = spdomain.jvm
lazy val spdomain_js = spdomain.js

lazy val spcomm = project
  .dependsOn(spdomain_jvm)
  .settings(commonSettings)
  .settings(libraryDependencies ++= commDependencies.value)

lazy val spcore = project
  .dependsOn(spdomain_jvm, spcomm)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= domainDependencies.value)
  .settings(libraryDependencies ++= commDependencies.value)


lazy val spgui = project
  .dependsOn(spdomain_js)
  .settings(commonSettings: _*)
  .settings(jsSettings: _*)
  .enablePlugins(ScalaJSPlugin)

lazy val sp1 = project

val backendDeps: Seq[ClasspathDep[ProjectReference]] = Seq(spdomain_jvm, spcomm, spcore)
val frontendDeps: Seq[ClasspathDep[ProjectReference]] = Seq(spdomain_js, spgui)


lazy val control_api = crossProject.crossType(CrossType.Pure).in(file("spcontrol/api"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings
  )
  .dependsOn(spdomain)

lazy val control_api_jvm = control_api.jvm
lazy val control_api_js = control_api.js

lazy val control_backend = project.in(file("spcontrol/backend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    libraryDependencies ++= commDependencies.value,
    commonSettings,
    serviceSettings
  )
  .dependsOn(control_api_jvm)
  .dependsOn(backendDeps:_*)


lazy val control_frontend = project.in(file("spcontrol/frontend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings,
    jsSettings
  )
  .dependsOn(control_api_js)
  .dependsOn(frontendDeps:_*)
  .enablePlugins(ScalaJSPlugin)



lazy val erica_api = crossProject.crossType(CrossType.Pure).in(file("sperica/api"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings
  )
  .dependsOn(spdomain)

lazy val erica_api_jvm = erica_api.jvm
lazy val erica_api_js = erica_api.js

lazy val erica_backend = project.in(file("sperica/backend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    libraryDependencies ++= commDependencies.value,
    commonSettings,
    serviceSettings
  )
  .dependsOn(erica_api_jvm)
  .dependsOn(backendDeps:_*)


lazy val erica_frontend = project.in(file("sperica/frontend"))
  .settings(
    libraryDependencies ++= domainDependencies.value,
    commonSettings,
    jsSettings
  )
  .dependsOn(erica_api_js)
  .dependsOn(frontendDeps:_*)
  .enablePlugins(ScalaJSPlugin)
