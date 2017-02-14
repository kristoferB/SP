package sp.server

import akka.actor.ActorSystem
import scala.io.StdIn

/**
  * Created by kristofer on 2017-01-02.
  */
object SeedNode extends App {
  implicit val system = ActorSystem("SP")

  println("Starting SP seed node")

  val cluster = akka.cluster.Cluster(system)
  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
    case x =>
      cluster.leave(cluster.selfAddress)
      system.terminate()
  }

}
