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
  implicit val sys = ActorSystem("SPCore")
  implicit val ec = sys.dispatcher
  val webServer = new LaunchGUI(sys)
  val f = webServer.launch

  println("Starting SP Core node")
  StdIn.readLine()

  f.flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => sys.terminate()) // and shutdown when done

}
