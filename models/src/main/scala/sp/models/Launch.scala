package sp.models

import akka.actor._
import sp.opcMilo._
import sp.labkit.labkit.OPC

object Launch extends App {
  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  val opcruntime = system.actorOf(OpcUARuntime.props, "OpcUARuntime")
  system.actorOf(OPC.props(opcruntime), "OPC")

}
