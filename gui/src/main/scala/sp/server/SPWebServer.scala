package sp.server

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import spray.routing._

/**
 * Created by Kristofer on 2014-06-19.
 */
class SPWebServer extends Actor with SPRoute {

  import akka.pattern.{ ask, pipe }
  import system.dispatcher

  val eh = context.actorSelection("../eventHandler")

  override val eventHandler = context.actorSelection("../eventHandler")
  override val modelHandler = context.actorSelection("../modelHandler")
  override val runtimeHandler = context.actorSelection("../runtimeHandler")
  override val serviceHandler = context.actorSelection("../serviceHandler")
  override val userHandler = context.actorSelection("../userHandler")
  implicit val system = context.system

  val webFolder: String = sp.system.SPActorSystem.settings.webFolder
  val srcFolder: String = if(sp.system.SPActorSystem.settings.devMode)
    sp.system.SPActorSystem.settings.devFolder else sp.system.SPActorSystem.settings.buildFolder

  def actorRefFactory = context
  def receive = runRoute(api ~ staticRoute)

  def staticRoute: Route =  {
    pathEndOrSingleSlash {
      getFromFile(srcFolder + "/index.html")
    } ~
    getFromDirectory(webFolder) ~
    getFromDirectory(srcFolder) ~
    getFromFile(srcFolder + "/index.html")
  }

}