package sp.robotServices.launcher

import java.text.SimpleDateFormat

import akka.pattern.ask
import akka.actor._
import akka.util.Timeout
import com.codemettle.reactivemq.ReActiveMQExtension
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model.{ AMQMessage, Topic }
import org.json4s.native.JsonMethods.parse
import org.json4s.{ DefaultFormats, JValue }
import sp.domain.Logic._
import sp.domain._
import sp.robotServices.core.Domain.ActivityEvent
import sp.system.SPActorSystem.settings
import sp.system.{ SPService, ServiceSupport }
import sp.system.messages.{ GetModels, KeyDefinition, TransformValue, UpdateIDs, _ }
import sp.robotServices.core.Helpers._

/**
 * Created by ashfaqf on 2/1/17.
 */

case class createOperationsFromActivitiesSetup(command: String) //, AMQBusIP: String, AMQBusUsername: String, AMQBusPass:String, AMQBusTopic:String)

object createOperationsFromActivities extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "starts the robot runtime for processing and aggregation of data"
    ),
    "setup" -> SPAttributes(
      "command" -> KeyDefinition("String", List("StartRuntime", "StopRuntime"), Some("StartRuntime"))
    // "AMQBusIP" -> KeyDefinition("String", List(), Some(settings.activeMQ)),
    // "AMQBusUsername" -> KeyDefinition("String",List(),Some("admin")),
    // "AMQBusPass" -> KeyDefinition("String",List(),Some("admin")),
    // "AMQBusTopic" -> KeyDefinition("String",List(),Some(settings.activeMQTopic))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[createOperationsFromActivitiesSetup]("setup"))
  )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props(modelHandler: ActorRef) = Props(classOf[createOperationsFromActivities], modelHandler)

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}

class createOperationsFromActivities(modelHandler: ActorRef) extends Actor with ServiceSupport {
  val customDateFormat = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  }
  implicit val formats = customDateFormat ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization
  println("starting create ops serive")
  val mid = ID.makeID("83b5b019-e950-4297-aea3-8762db19cca2").get //ID.newID
  modelHandler ! CreateModel(mid, "RobotServiceModel")
  var theBus: Option[ActorRef] = None
  ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${settings.activeMQ}:${settings.activeMQPort}")
  import scala.concurrent.duration._

  var resources: List[String] = List.empty
  var opToAttrMap: Map[String, Operation] = Map.empty
  implicit val timeout = Timeout(5 seconds)

  
  def receive = {
    case r @ Request(service, attr, ids, reqID) =>
      println("Got request")
      val c = r.attributes.getAs[ServiceHandlerAttributes]("core")


      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      val setup = transform(createOperationsFromActivities.transformTuple)

      replyTo ! Response(List(), SPAttributes("info" -> "Runtime Started"), service, reqID)

    case ConnectionEstablished(request, c) => {
      println("connected:" + request)
      c ! ConsumeFromTopic(settings.activeMQTopic)
      theBus = Some(c)
    }
    case ConnectionFailed(request, reason) => {
      println("failed:" + reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      val json: JValue = parse(body.toString)
      if (json.has("activityId")) {
        val op: ActivityEvent = json.extract[ActivityEvent]
        updateMapWithIDable(op)
        modelHandler ! UpdateIDs(mid, opToAttrMap.values.toList)
      }
    }
  }
  def updateMapWithIDable(op: ActivityEvent): Unit = {


    def updateIDable(idable: IDAble) {
      val times = idable.attributes.getAs[SPAttributes]("Times")


    }
    if (opToAttrMap.contains(op.name)) {
      val opVal = opToAttrMap(op.name)
      val updIDable = updateIDable(opVal)
      opToAttrMap += (op.name -> opVal.copy(attributes = (opVal.attributes transformField{case ("Times",value)=> ("Times",SPValue(value.children :+ op.time)) }).to[SPAttributes].getOrElse(SPAttributes())))
    }
    else
      {
        val name = op.name + s"${if(op.isStart) "_start" else "_stop"}"
        opToAttrMap += op.name -> Operation(name, List(),SPAttributes("isStart" -> op.isStart, "Times" -> List(op.time), "type" -> op.`type`, "robotId" -> op.robotId))
      }


  }
}
