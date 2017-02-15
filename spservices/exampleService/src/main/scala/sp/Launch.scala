package sp

import akka.actor._
import sp.domain._
import sp.example.{API_ExampleService, ExampleService}
import sp.messages._

import scala.util.{Failure, Success, Try}




object Launch extends App {

  implicit val system = ActorSystem("SP")

  val cluster = akka.cluster.Cluster(system)
  cluster.registerOnMemberUp{
    // Start all you actors here.
    system.actorOf(ExampleService.props, API_ExampleService.service)
  }

  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
    case x =>
      cluster.leave(cluster.selfAddress)
      system.terminate()
  }


}
