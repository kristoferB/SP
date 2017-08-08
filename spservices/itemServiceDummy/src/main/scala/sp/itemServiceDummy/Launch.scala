package sp.itemServiceDummy

import akka.actor._

object Launch extends App {
  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  system.actorOf(ItemServiceDummy.props, ItemServiceDummy.service)

  val cluster = akka.cluster.Cluster(system)
  scala.io.StdIn.readLine("Press ENTER to exit itemEditorService.\n") match {
    case x =>
      cluster.leave(cluster.selfAddress)
      system.terminate()
  }
}
