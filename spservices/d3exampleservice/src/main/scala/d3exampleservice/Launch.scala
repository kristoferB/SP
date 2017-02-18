package sp.d3exampleservice

import akka.actor._


object Launch extends App {
  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  system.actorOf(D3ExampleService.props, D3ExampleService.service)

  val cluster = akka.cluster.Cluster(system)
  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
    case x =>
      cluster.leave(cluster.selfAddress)
      system.terminate()
  }

}
