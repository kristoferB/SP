package sp.opcRunner

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import org.json4s.JsonAST.{JValue,JBool,JInt,JString}
import org.json4s.DefaultFormats
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit
import sp.opcMilo.OpcUARuntime
import org.joda.time.DateTime

object OPCUARunner {
  def props = Props(classOf[OPCUARunner])
}

case class OPCUAStateChange(state: Map[String, SPValue], time: String)
case class OPCUAWrite(identifier: String, value: SPValue)

class OPCUARunner extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator
  val opc = context.system.actorOf(OpcUARuntime.props, "OpcUARuntime")

  var connected = false
  var subscribed = false
  var subscribedTo: List[String] = List()
  var nodes: Map[String,String] = Map()

  mediator ! Subscribe("OPCState", self)
  mediator ! Subscribe("OPCUARunnerCommands", self)

  self ! "connect"

  def receive = {
    case "connect" =>
      opc ! SPAttributes("cmd" -> "connect", "url" -> "opc.tcp://localhost:12686")

    case OPCUAWrite(ident, value) =>
      opc ! SPAttributes("cmd" -> "write", "node" -> ident, "value" -> value)

    case attr: SPAttributes =>
      // check connection
      val connectionstatus = attr.getAs[Boolean]("connected").getOrElse(false)
      if(!connected && connectionstatus) {
        connected = true
        opc ! SPAttributes("cmd" -> "getNodes")
        println("connected")
      }
      if(!connected && !connectionstatus) {
        // try again in five seconds
        context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, "connect")
        println("could not connect, waiting")
      }

      // node list
      val newnodes = attr.getAs[Map[String,String]]("nodes").getOrElse(Map())
      if(nodes.isEmpty && newnodes.nonEmpty) {
        nodes = newnodes
        // subscribe to all
        subscribedTo = nodes.map(_._1).toList
        opc ! SPAttributes("cmd" -> "subscribe", "nodes" -> subscribedTo)
        subscribed = true
        println("got nodes, subscribing")
      }

      // state update
      val time = attr.getAs[String]("timeStamp").getOrElse("")
      val state = attr.getAs[Map[String, SPValue]]("state").getOrElse(Map()).filter(p=>subscribedTo.contains(p._1))
      mediator ! Publish("OPCUARunner", OPCUAStateChange(state, time))
    case _ =>
  }

}
