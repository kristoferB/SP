package sp.server

import akka.actor._
import spray.routing._

/**
 * Created by Kristofer on 2014-06-19.
 */
class SPWebServer extends Actor with SPRoute {
  override val modelHandler : ActorRef= sp.system.SPActorSystem.modelHandler
  override val runtimeHandler : ActorRef= sp.system.SPActorSystem.runtimeHandler
  override val serviceHandler : ActorRef= sp.system.SPActorSystem.serviceHandler
  override val userHandler : ActorRef= sp.system.SPActorSystem.userHandler

  def actorRefFactory = context
  def receive = runRoute(api)
  import sp.system._

  val baseDir = "./gui/webapp"

  def staticRoute: Route = {
    path("") {
      getFromFile(baseDir + "/index.html")
    } ~ {
      getFromDirectory(baseDir)
    }
  }

}