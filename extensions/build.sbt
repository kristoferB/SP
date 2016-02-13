libraryDependencies ++= Seq(
  "com.typesafe.akka"  %%  "akka-camel"   % "2.4.1",
  "org.apache.activemq" % "activemq-core" % "5.7.0",
  "org.apache.activemq" % "activemq-camel" % "5.11.1"
)
//  "log4j" % "log4j" % "1.2.17" Removed by Patrik 150625, because of unwanted print outs when running Supremica.
