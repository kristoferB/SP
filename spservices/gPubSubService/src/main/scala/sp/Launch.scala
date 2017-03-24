package sp

import akka.actor._
import sp.domain._
import sp.gPubSub.{API_GPubSubService, GPubSubService}
import sp.messages._

import scala.util.{Failure, Success, Try}




object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("GPubSubService node has joined the cluster")
    system.actorOf(GPubSubService.props, API_GPubSubService.attributes.service)

  }
  cluster.registerOnMemberRemoved{
    println("GPubSubService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()


}