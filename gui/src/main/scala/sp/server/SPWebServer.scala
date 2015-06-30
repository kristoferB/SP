package sp.server

import akka.actor._
import sp.system.SPActorSystem._
import spray.routing._

/**
 * Created by Kristofer on 2014-06-19.
 */
class SPWebServer extends Actor with SPRoute {
  override val modelHandler : ActorRef= sp.system.SPActorSystem.modelHandler
  override val runtimeHandler : ActorRef= sp.system.SPActorSystem.runtimeHandler
  override val serviceHandler : ActorRef= sp.system.SPActorSystem.serviceHandler
  override val userHandler : ActorRef= sp.system.SPActorSystem.userHandler
  implicit val system = context.system

  val webFolder: String = settings.webFolder
  val srcFolder: String = if(settings.devMode) settings.devFolder else settings.buildFolder

  def actorRefFactory = context
  def receive = runRoute(api ~ staticRoute)

  def staticRoute: Route =  {
    pathEndOrSingleSlash {
      getFromFile(srcFolder + "/index.html")
    } ~
    getFromDirectory(webFolder) ~
    getFromDirectory(srcFolder)
  }

}