package sp.server

import akka.actor.ActorSystem
import scala.io.StdIn

/**
  * Created by kristofer on 2017-01-02.
  */
object SeedNode extends App {
  implicit val system = ActorSystem("SP")

  println("Starting SP seed node")

  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("Seed node has joined the cluster")

  }
  cluster.registerOnMemberRemoved{
    println("Seed node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()


}
