package sp.EricaEventLogger

import akka.actor._

import org.joda.time.DateTime
import scala.collection.mutable

import elastic.DataAggregation
import sp.gPubSub.API_Data.EricaEvent

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

  val allEvents = mutable.ListBuffer[EricaEvent]()

  override def receive = {
    //case "done" => allEvents.sortBy(ev => DateTime.parse(ev.Start).getMillis).foreach(ev => println(ev))
    case "done" => allEvents.sortBy(ev => DateTime.parse(ev.Start).getMillis).foreach(ev => evLogger ! ev)
    case x: String => allEvents ++= dataAgg.handleMessage(x)
  }
}
