package sp.opcua

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor._

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here
    println("OPC UA node has joined the cluster.")

    val dh = system.actorOf(DriverHandler.props, "OPCUA")

    cluster.registerOnMemberRemoved{
      println("OPC UA node has been removed from the cluster")
      dh ! "stop"
    }
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")

  system.terminate()
  Await.ready(system.whenTerminated, Duration(30, SECONDS))
  try {
    // cleanup milo crap
    import sp.milowrapper.MiloOPCUAClient
    MiloOPCUAClient.destroy()
  } catch {
    case e: Exception =>
      println("OPCUA crash - " + e.getMessage())
  }
}
