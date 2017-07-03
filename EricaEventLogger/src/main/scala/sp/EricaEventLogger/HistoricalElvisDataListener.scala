package sp.EricaEventLogger

import akka.actor._

import elastic.DataAggregation

object HistoricalElvisDataListener {
  def props = Props(classOf[HistoricalElvisDataListener])
}

class HistoricalElvisDataListener extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.Subscribe
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("historical-elvis-data", self)

  val dataAgg = new DataAggregation
  val evLogger = context.actorOf(Props[Logger], "EricaEventLogger")

  override def receive = {
    case x: String => dataAgg.handleMessage(x).foreach(ev => evLogger ! ev)
  }
}
