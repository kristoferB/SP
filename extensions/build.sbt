libraryDependencies ++= Seq(
  "com.codemettle.reactivemq" %% "reactivemq" % "0.5.5",
  "org.apache.activemq" % "activemq-client"   % "5.13.3",
  "de.ummels" %% "scala-prioritymap" % "0.5.0",

  "io.netty" % "netty-all" % "4.1.6.Final",
  "com.codepoetics" % "protonpack" % "1.1",
  "org.jooq" % "jool" % "0.9.12",
  "io.netty" % "netty-handler" % "4.1.6.Final",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "com.google.guava" % "guava" % "20.0",
  "com.google.code.findbugs" % "jsr305" % "3.0.1",
  "org.eclipse.milo" % "sdk-client" % "0.1.0",
  "org.scala-lang.modules" % "scala-java8-compat_2.11" % "0.3.0",

  "com.github.tototoshi" %% "scala-csv" % "1.3.4",
  "com.github.gphat" %% "wabisabi" % "2.1.9"
)
//  "log4j" % "log4j" % "1.2.17" Removed by Patrik 150625, because of unwanted print outs when running Supremica.
