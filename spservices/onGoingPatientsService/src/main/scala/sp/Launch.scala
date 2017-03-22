package sp

import akka.actor._
import sp.domain._
import sp.onGoingPatients.{API_OnGoingPatientsService, OnGoingPatientsService}
import sp.messages._

import scala.util.{Failure, Success, Try}




object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("OnGoingPatientsService node has joined the cluster")
    system.actorOf(OnGoingPatientsService.props, API_OnGoingPatientsService.attributes.service)

  }
  cluster.registerOnMemberRemoved{
    println("OnGoingPatientsService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()

}
