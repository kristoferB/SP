package sp.plc

import akka.actor._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object PLCControl extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "PLC" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "busIP" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "publishTopic" -> KeyDefinition("String", List(), Some("command")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("response"))
    ),
    "command" -> SPAttributes(
      "startOP" -> KeyDefinition("Option[ID]", List(), None),
      "setState" -> KeyDefinition("Option[State]", List(), None)
    )
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[PLCControl], eventHandler)
}


case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class PLCControl(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher

  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  val serviceID = ID.newID

  println("make me")

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(PLCControl.transformTuple._1)
      val commands = transform(PLCControl.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      if (setup.isEmpty){
        setup = Some(s)
        serviceName = Some(service)
        ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.busIP}:61616")
      }

      for {
        bus <- theBus
        s <- setup
      } yield {
        commands.getAs[ID]("startOP").foreach{ id =>
          // fixa med op
          val json = SPAttributes("isa"->"startOP","start"->id).toJson
          bus ! SendMessage(Topic(s.publishTopic), AMQMessage(json))
        }
        commands.getAs[State]("setState").foreach{ state =>
          val json = SPAttributes("isa"->"setState","state"->state).toJson
          bus ! SendMessage(Topic(s.publishTopic), AMQMessage(json))
        }
      }


      val respAttr = if (theBus.isEmpty)
        SPAttributes("theBus"->"Connecting")
      else
        SPAttributes("theBus"->"Connected")

      replyTo ! Response(List(), respAttr, service, serviceID)

    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.map{ s=>
        c ! ConsumeFromTopic(s.subscribeTopic)
        c ! ConsumeFromTopic(s.publishTopic)
        theBus = Some(c)
        eventHandler ! Progress(SPAttributes("theBus"-> "Connected"), serviceName.get, serviceID)
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      val resp = SPAttributes.fromJson(body.toString)
      println("PLC Control got: "+body)
      // fixa här
      eventHandler ! Response(List(), SPAttributes("resp"->resp), serviceName.get, serviceID)
    }
    case x => {
      println("PLC control got message "+x)
      //sender() ! SPError("What do you whant me to do? "+ x)
    }
  }


}



