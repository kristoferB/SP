package sp.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

/**
  * Created by kristofer on 2017-01-02.
  */
object ServeSPUI extends App{
  implicit val sys = ActorSystem("SP")
  implicit val ec = sys.dispatcher
  val webServer = new LaunchGUI(sys)
  val f = webServer.launch
  val cluster = akka.cluster.Cluster(sys)

  cluster.registerOnMemberUp {

    // Start all you actors here.
    println("SP core node has joined the cluster")
    sys.actorOf(sp.service.ServiceHandler.props)

  }
  cluster.registerOnMemberRemoved{
    println("SP core node has been removed from the cluster")
  }

  println("Starting SP Core node")

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")

  cluster.leave(cluster.selfAddress)


  scala.io.StdIn.readLine("Press ENTER to exit application.\n")

  f.flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete{_ =>
      sys.terminate()
     } // and shutdown when done

}
