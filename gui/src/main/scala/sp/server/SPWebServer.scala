package sp.server

import akka.actor._
import spray.routing._


/**
 * Created by Kristofer on 2014-06-19.
 */
class SPWebServer extends Actor with SPRoute {
  override val modelHandler : ActorRef= sp.system.SPActorSystem.modelHandler
  def actorRefFactory = context
  def receive = runRoute(api ~ staticRoute)
  import sp.system._

  def staticRoute: Route = {
    //path("")(getFromResource("webapp/index.html")) ~ getFromResourceDirectory("webapp")
    path("")(getFromFile(s"${SPActorSystem.settings.webfolder}/index.html")) ~ getFromDirectory(SPActorSystem.settings.webfolder)
  }
}


