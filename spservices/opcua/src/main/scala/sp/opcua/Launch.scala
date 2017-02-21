package sp.opcua

import akka.actor._
import sp.opcMilo._
import scala.concurrent.duration._
import scala.concurrent.Await

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here
    println("OPC UA node has been added to the cluster")

    val opcruntime = system.actorOf(OpcUARuntime.props, "OpcUARuntime")
  }
  cluster.registerOnMemberRemoved{
    println("OPC UA node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  Await.ready(system.whenTerminated, Duration(10, SECONDS))
  // cleanup milo crap
  MiloOPCUAClient.destroy()
  system.terminate()

}
