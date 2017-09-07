package sp.erica

import akka.actor._

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)
  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  sp.SPCore.launch(system)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    log.info("GPubSubService node has joined the cluster")
    system.actorOf(WidgetDevice.props)
    system.actorOf(GPubSubDevice.props, API_PatientEvent.service)

  }
  cluster.registerOnMemberRemoved{
    log.info("GPubSubService node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()
}
