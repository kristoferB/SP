//package sp.server
//
//import akka.actor._
//import akka.util.Timeout
//import sp.system.PubActor
//
//import scala.concurrent.duration._
//
///**
// * Created by Kristofer on 2014-06-19.
// */
//class SPWebServer extends Actor with SPRoute {
//
//  import akka.pattern.{ ask, pipe }
//  import system.dispatcher
//
//
//  override val modelHandler = context.actorOf(PubActor.props("modelHandler"))
//  override val serviceHandler = context.actorOf(PubActor.props("serviceHandler"))
//  override val eventHandler = context.actorOf(PubActor.props("eventHandler"))
//  override val runtimeHandler = context.actorOf(PubActor.props("runtimeHandler"))
//  override val userHandler = context.actorOf(PubActor.props("userHandler"))
//  implicit val system = context.system
//
//
//
//  val webFolder: String = sp.system.SPActorSystem.settings.webFolder
//  val srcFolder: String = if(sp.system.SPActorSystem.settings.devMode)
//    sp.system.SPActorSystem.settings.devFolder else sp.system.SPActorSystem.settings.buildFolder
//  def actorRefFactory = context
//  def receive = null//runRoute(api ~ staticRoute)
//
////  def staticRoute: Route =  {
////    pathEndOrSingleSlash {
////      getFromFile(srcFolder + "/index.html")
////    } ~
////    getFromDirectory(webFolder) ~
////    getFromDirectory(srcFolder) ~
////    getFromFile(srcFolder + "/index.html")
////  }
//
//}
//
//import akka.cluster.pubsub._
//class Publisher(topic: String) extends Actor {
//  import DistributedPubSubMediator.Publish
//  val mediator = DistributedPubSub(context.system).mediator
//
//  def receive = {
//    case in ⇒
//      mediator forward Publish(topic, in)
//  }
//}