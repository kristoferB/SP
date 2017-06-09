package sp

import akka.actor._
import sp.domain._
import sp.elvisdatahandler.{API_PatientEvent, ElvisDataHandlerDevice}
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
    println("ElvisDataHandlerService node has joined the cluster")
    system.actorOf(ElvisDataHandlerDevice.props, API_PatientEvent.attributes.service)
    system.actorOf(sp.gPubSub.GPubSubDevice.props)
    system.actorOf(sp.widgetservice.WidgetDevice.props)

  }
  cluster.registerOnMemberRemoved{
    println("ElvisDataHandlerService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  f.flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete{_ =>
    system.terminate()
  } // and shutdown when done

}
