package sp.opcua

import java.util.UUID
import java.util.concurrent.TimeUnit
import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import org.joda.time.DateTime
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import sp.messages._
import sp.messages.Pickles._
import scala.util.{Failure, Success, Try}
import sp.milowrapper.{MiloOPCUAClient, StateUpdate}

// the vd api
import sp.opcua.{APIVirtualDevice => vdapi}


object OpcUARuntime {
  def props(name: String, id: UUID, setup: SPAttributes) = Props(classOf[OpcUARuntime], name, id, setup)
}

class OpcUARuntime(name: String, id: UUID, setup: SPAttributes) extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("driverCommands", self)

  val client = new MiloOPCUAClient()

  self ! "connect"

  def receive = {
    case "connect" =>
      val url = setup.getAs[String]("url").getOrElse("")
      if(!client.connect(url)) {
        println("opcua driver could not connect, trying again soon...")
        context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, "connect")
      } else {
        println("opcua driver connected, subscribing to resources")
        val identifiers = setup.getAs[List[String]]("identifiers").getOrElse(List())
        client.subscribeToNodes(identifiers, self)
        println("opcua subscribed to " + identifiers.length + " identifiers")
      }

    case "disconnect" =>
      println("OPCUA - disconnecting")
      if(client.isConnected) client.disconnect()
      println("OPCUA - disconnected")

    case x: String =>
      // SPMessage uses the APIParser to parse the json string
      SPMessage.fromJson(x) match {
        case Success(mess) =>
          for {
            h <- mess.getHeaderAs[SPHeader]
            b <- mess.getBodyAs[vdapi.DriverCommand] if b.id == id
          } yield {
            b match {
              case vdapi.DriverCommand(name, id, state) if client.isConnected =>
                state.foreach{case(node,value) => client.write(node, value)}
              case _ =>
            }
          }
        case Failure(err) => {}
      }
    case StateUpdate(activeState) =>
      val header = SPHeader(from = name)
      val stateWithTime = activeState + ("timestamp" -> SPValue(client.getCurrentTime.toString))
      val body = vdapi.DriverStateChange(name, id, stateWithTime, false)
      mediator ! Publish("driverEvents", SPMessage.makeJson(header, body))
    case _ => sender ! APISP.SPError("Ill formed request")
  }

}
