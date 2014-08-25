package sp.server

import akka.io.IO
import spray.can.Http
import akka.actor.Props
import sp.system.SPActorSystem._

/**
 * Used by the SP launcher file
 * Created by Kristofer on 2014-06-19.
 */
object LaunchGUI {//extends MySslConfiguration {
  def launch = {

    val interface = settings.interface
    val port = settings.port

    val service = system.actorOf(Props(classOf[SPWebServer]), "sprayServer")

    IO(Http) ! Http.Bind(service, interface, port)

    Console.println(s"Server started ${system.name}, $interface:$port")
    Console.println("Type `exit` to exit....")

    Console.readLine() // wait for enter to exit
    system.shutdown()


  }
}
