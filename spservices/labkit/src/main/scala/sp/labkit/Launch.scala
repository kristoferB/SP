package sp.labkit

import akka.actor._
import sp.models.{ModelActor, ModelMaker}




object Launch extends App {

  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  system.actorOf(OPMakerLabKit.props, "opMakerLabKit")
  system.actorOf(ProductAggregator.props, "ProductAggregator")
  system.actorOf(ResourceAggregator.props, "ResourceAggregator")





}
