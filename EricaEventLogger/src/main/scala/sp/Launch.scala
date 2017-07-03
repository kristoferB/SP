package sp

import akka.actor._
import sp.EricaEventLogger.HistoricalElvisDataListener

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    println("HistoricalElvisDataListener node has joined the cluster")
    system.actorOf(HistoricalElvisDataListener.props, "HistoricalElvisDataListener")
  }
  cluster.registerOnMemberRemoved{
    println("HistoricalElvisDataListener node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()
}
