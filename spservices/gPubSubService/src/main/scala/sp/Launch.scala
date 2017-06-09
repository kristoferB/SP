package sp

import akka.actor._
import sp.domain._
import sp.gPubSub.{API_PatientEvent, GPubSubDevice}
import sp.widgetservice.{WidgetDevice}
import sp.messages._

import scala.util.{Failure, Success, Try}

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)
  implicit val ec = system.dispatcher
  val webServer = new sp.server.LaunchGUI(system)
  val f = webServer.launch

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("GPubSubService node has joined the cluster")
    system.actorOf(WidgetDevice.props)
    system.actorOf(GPubSubDevice.props, API_PatientEvent.attributes.service)

  }
  cluster.registerOnMemberRemoved{
    println("GPubSubService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  f.flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete{_ =>
    system.terminate()
  }
}
