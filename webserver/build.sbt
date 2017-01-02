libraryDependencies ++= Seq(
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-testkit" % "1.3.3",
  "io.spray" %%  "spray-json" % "1.3.2"
)

resolvers += "spray repo" at "http://repo.spray.io"