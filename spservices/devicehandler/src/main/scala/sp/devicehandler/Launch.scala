package sp.devicehandler

import akka.actor._
import sp.labkit.{OPC, OPMakerLabKit, ProductAggregator, ResourceAggregator}


object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here
    println("labkit node has joined the cluster")
    system.actorOf(DeviceHandler.props, "deviceHandler")
  }

  cluster.registerOnMemberRemoved{
    println("labkit node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()

}
