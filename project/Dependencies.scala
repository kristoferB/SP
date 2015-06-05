import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io/",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

  )
  
  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val akkaActor     = "com.typesafe.akka"    %%  "akka-actor"                  % "2.3.6"
  val akkaSlf4j     = "com.typesafe.akka"    %%  "akka-slf4j"                  % "2.3.6"
  val akkaTestKit   = "com.typesafe.akka"    %%  "akka-testkit"                % "2.3.6" % "test"
  val akkaPersistence= "com.typesafe.akka"    %% "akka-persistence-experimental"% "2.3.6"
  val parboiled     = "org.parboiled"        %%  "parboiled-scala"             % "1.1.6"
  val shapeless     = "com.chuusai"          %%  "shapeless"                   % "1.2.4"
  val scalatest     = "org.scalatest"        %%  "scalatest"                   % "2.2.1"
  //val specs2        = "org.specs2"           %%  "specs2"                      % "2.4.6"
  val sprayJson     = "io.spray"             %%  "spray-json"                  % "1.2.5"
  val logback       = "ch.qos.logback"       %   "logback-classic"             % "1.1.1"

  val breeze = "org.scalanlp" %% "breeze" % "0.10"
  //val breezeNative = "org.scalanlp" %% "breeze-natives" % "0.10"

  val sprayCan =  "io.spray" %% "spray-can" % "1.3.1"
  val sprayRouting ="io.spray" %% "spray-routing" % "1.3.1"
  val sprayTest ="io.spray" %% "spray-testkit" % "1.3.1" % "test"
  
  val nscalatime = "com.github.nscala-time" %% "nscala-time" % "1.4.0"
  //val parser =  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2"

  // for activemq test
  val akkaCamel     = "com.typesafe.akka"  %%  "akka-camel"                  % "2.3.6"
  val activeMQ      = "org.apache.activemq" % "activemq-core" % "5.7.0"
  val activeMQCamel = "org.apache.activemq" % "activemq-camel" % "5.11.1"

}
