package sp.server

import akka.actor._

sealed trait CommandTheListener
case class ListenToTopic(topic: String) extends CommandTheListener
case class FilterMessages(topic: String, filter: String => Boolean) extends CommandTheListener

//
///**
//  * Created by kristofer on 2017-01-10.
//  */
//class WebSocketActor(replyTo: ActorRef) extends Actor {
//  var topics: Set[String] = Set()
//
//  def receive = {
//    case
//  }
//
//}
