package sp.server

import akka.io.IO
import spray.can.Http
import akka.actor._

/**
 * Used by the SP launcher file
 * Created by Kristofer on 2014-06-19.
 */
object LaunchGUI extends App {//extends MySslConfiguration {
  def launch = {

  implicit val system = sp.system.SPActorSystem.system

    val interface = system.settings.config getString "sp.interface"
    val port = system.settings.config getInt "sp.port"

    val service = system.actorOf(Props(classOf[SPWebServer]), "sprayServer")

    IO(Http) ! Http.Bind(service, interface, port)

    println(s"Server started ${system.name}, $interface:$port")

//  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
//    case x => system.terminate()
//  }



  }
  launch
}
