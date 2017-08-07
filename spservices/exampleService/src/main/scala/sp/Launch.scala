package sp

import akka.actor._
import sp.example._

import scala.util.{Failure, Success, Try}




object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("ExampleService node has joined the cluster")
    system.actorOf(ExampleService.props, sp.example.ExampleServiceInfo.attributes.service)

  }
  cluster.registerOnMemberRemoved{
    println("ExampleService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()


}
