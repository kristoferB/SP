package sp.server

import akka.io.IO
import spray.can.Http

/**
 * Created by Kristofer on 2014-06-19.
 */
class Boot extends App {

  import sp.system.SPActorSystem._
  val interface = settings.interface
  val port = settings.port

  IO(Http) ! Http.Bind(service, interface, port)

  Console.println(s"Server started ${system.name}, $interface:$port")
  Console.println("Type `exit` to exit....")

  //test

  waitForExit()
  system.shutdown()

  private def waitForExit() = {
    def waitEOF(): Unit = Console.readLine() match {
      case "exit" => system.shutdown()
      case _ => waitEOF()
    }
    waitEOF()
  }

}
