package sp

import akka.actor._
import sp.domain._
import sp.example.{TimelineExampleService, Timeline_API}
import sp.messages._

import scala.util.{Failure, Success, Try}




object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("TimelineExampleService node has joined the cluster")
    system.actorOf(TimelineExampleService.props, Timeline_API.attributes.service)

  }
  cluster.registerOnMemberRemoved{
    println("TimelineExampleService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()


}
