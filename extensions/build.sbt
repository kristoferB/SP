libraryDependencies ++= Seq(
  "com.codemettle.reactivemq" %% "reactivemq" % "0.5.5",
  "org.apache.activemq" % "activemq-client"   % "5.13.3",
  "de.ummels" %% "scala-prioritymap" % "0.5.0"
)
//  "log4j" % "log4j" % "1.2.17" Removed by Patrik 150625, because of unwanted print outs when running Supremica.
