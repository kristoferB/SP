package sp

import akka.actor._
import sp.EricaEventLogger.Logger

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    println("EricaEventLogger node has joined the cluster")
    system.actorOf(Logger.props, "EricaEventLogger")
  }
  cluster.registerOnMemberRemoved{
    println("EricaEventLogger node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()
}
