import sbt._
import Keys._


object Build extends Build {
  import BuildSettings._
  import Dependencies._

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }

  lazy val defaultDepend = Seq(akkaActor,
    akkaSlf4j,
    logback,
    scalatest,
    akkaTestKit,
    nscalatime)

  lazy val coreDepend = Seq(akkaPersistence)
    //parser)


  lazy val sprayDepend = Seq(sprayCan,
    sprayRouting,
    sprayJson)

  lazy val root = project.in( file(".") )
    .aggregate(core, gui)
    .settings(basicSettings: _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)


  lazy val core = project.in(file("core"))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= (defaultDepend ++ coreDepend))
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)


  lazy val gui = project.in(file("gui"))
    .dependsOn(core)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= (defaultDepend ++ sprayDepend))
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    

  lazy val launch = project.in(file("launch"))
    .dependsOn(core, gui, extensions)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= (defaultDepend :+ akkaPersistence))

  lazy val extensions = project.in(file("extensions"))
    .dependsOn(core)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= (defaultDepend))

  //run in Compile <<= (run in Compile in launch)
	
}