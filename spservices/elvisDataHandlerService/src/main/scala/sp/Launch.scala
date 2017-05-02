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
    system.actorOf(sp.waitingroomservice.WaitingRoomDevice.props)
    system.actorOf(sp.patientcardsservice.PatientCardsDevice.props)
    system.actorOf(sp.placediagramservice.PlaceDiagramDevice.props)
    system.actorOf(sp.roomoverviewservice.RoomOverviewDevice.props)
    system.actorOf(sp.statusdiagramservice.StatusDiagramDevice.props)
    system.actorOf(sp.triagediagramservice.TriageDiagramDevice.props)
    system.actorOf(sp.coordinatordiagramservice.CoordinatorDiagramDevice.props)

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
