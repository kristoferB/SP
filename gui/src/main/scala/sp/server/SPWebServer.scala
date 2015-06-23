package sp.server

import akka.actor._
import sp.opc.ServerSideEventsDirectives._
import spray.routing._

import spray.http.HttpHeaders.RawHeader

/**
 * Created by Kristofer on 2014-06-19.
 */
class SPWebServer extends Actor with SPRoute {
  override val modelHandler : ActorRef= sp.system.SPActorSystem.modelHandler
  override val runtimeHandler : ActorRef= sp.system.SPActorSystem.runtimeHandler
  override val serviceHandler : ActorRef= sp.system.SPActorSystem.serviceHandler
  override val userHandler : ActorRef= sp.system.SPActorSystem.userHandler

  def actorRefFactory = context
  def receive = runRoute(api ~ sseApi ~ staticRoute)
  import sp.system._

  val baseDir = "/home/daniel/IDEAProjects/SP/gui/webapp"

  def staticRoute: Route = {
    path("") {
      getFromFile(baseDir + "/index.html")
    } ~ {
      getFromDirectory(baseDir)
    }
  }

  def idToInt(x: Any): Int = x match {
    case Some(y: String) => y.toInt
    case None => 0
  }

  val sseProcessor = actorRefFactory.actorOf(Props { new Actor {
    def receive = {
      case (channel: ActorRef, lastEventID: Any) =>
        // Print LastEventID if present
        //lastEventID.foreach(lei => println(s"LastEventID: $lei"))

        val lastEventIDno = idToInt(lastEventID)
        println(s"Last EventID as Int: $lastEventIDno")

        if(lastEventIDno < 1) {
          channel ! ServerSentEvent("R2 at table", "stateChange", "1")
        }

        // Simulate some work
        Thread.sleep(2000)

        if(lastEventIDno < 2) {
          channel ! ServerSentEvent("R2 moving", "stateChange", "2")
        }

        Thread.sleep(4000)

        if(lastEventIDno < 3) {
          channel ! ServerSentEvent("R2 at fixture", "stateChange", "3")
        }

    }
  }})

  val sseApi = path("sse") {
    respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
      sse { (channel, lastEventID) =>
        // Register a closed event handler
        channel ! RegisterClosedHandler( () => println("Connection closed !!!") )

        // Use the channel
        sseProcessor ! (channel, lastEventID)
      }
    }
  }

}