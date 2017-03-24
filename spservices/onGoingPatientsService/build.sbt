scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

//för wabasabi
resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/"

libraryDependencies ++= {
  val akkaV       = "2.4.17"
  val akkaStreamV = "2.0.5"
  val scalaTestV  = "2.2.5"

  Seq(
    "com.github.gphat" % "wabisabi_2.11" % "2.2.0",
    "com.typesafe"       % "config"                               % "1.3.0",
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
    "com.codemettle.reactivemq" %% "reactivemq"                   % "0.5.0",
    "org.apache.activemq" % "activemq-client"                     % "5.13.1",
    "org.json4s" %% "json4s-native"                               % "3.3.0",
    "org.json4s" %% "json4s-ext"                                  % "3.3.0",
    "org.json4s" %% "json4s-jackson"                              % "3.3.0",
    "com.github.nscala-time" %% "nscala-time"                     % "1.8.0",
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test"
  )
}
