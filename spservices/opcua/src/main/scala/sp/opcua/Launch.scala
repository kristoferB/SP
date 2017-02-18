package sp.opcua

import akka.actor._
import sp.opcMilo._
import scala.concurrent.duration._
import scala.concurrent.Await

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  // Add root actors used in node here
  cluster.registerOnMemberUp {
    val opcruntime = system.actorOf(OpcUARuntime.props, "OpcUARuntime")
    system.actorOf(OPC.props(opcruntime), "OPC")
  }

  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
    case x =>
      cluster.leave(cluster.selfAddress)
      system.terminate()
      // wait for actors to die
      Await.ready(system.whenTerminated, Duration(10, SECONDS))
      // cleanup milo crap
      MiloOPCUAClient.destroy()
  }
}
