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

  lazy val root = project.in( file(".") )
    .aggregate(core, gui)
    .settings(basicSettings: _*)


  lazy val core = project.in(file("core"))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= (defaultDepend :+ akkaPersistence))


  lazy val gui = project.in(file("gui"))
    .dependsOn(core)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= defaultDepend)


	
}