package sp.control

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


object OperationControl extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "busIP" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "publishTopic" -> KeyDefinition("String", List(), Some("commands")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("stateEvents")),
      "onlyAskStatus" -> KeyDefinition("Boolean", List(), Some(false))
    ),
    "command" -> SPAttributes(
      "execute" -> KeyDefinition("Option[ID]", List(), None),
      "parameters" -> KeyDefinition("Option[State]", List(), None)
    )
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[OperationControl], eventHandler)
}


case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String, onlyAskStatus: Boolean)

case class AdressValues(db: Int, byte: Int, bit: Int)
case class DBValue(id: ID, value: SPValue, valueType: String, address: AdressValues)



// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OperationControl(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher
  val serviceID = ID.newID

  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  var state: State = State(Map())
  var idMap: Map[ID, IDAble] = Map()


  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(OperationControl.transformTuple._1)
      val commands = transform(OperationControl.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      setupBus(s, rnr)
      sendCommands(commands)

      replyTo ! Response(List(), connectedAttribute, service, serviceID)

    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.map{ s=>
        c ! ConsumeFromTopic(s.subscribeTopic)
        theBus = Some(c)
        eventHandler ! Progress(SPAttributes("theBus"-> "Connected"), serviceName.get, serviceID)
        eventHandler ! Response(List(), SPAttributes("state"-> state), serviceName.get, serviceID)
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
    case ConnectionInterrupted(ca, x) => {
      println("connection closed")
      setup = None
    }

    case x => {
      println("PLC control got message "+x)
      //sender() ! SPError("What do you whant me to do? "+ x)
    }
  }

  var toggle = false
  def sendCommands(commands: SPAttributes) = {
    println(theBus)
    println(setup)
    println(commands)

    for {
      bus <- theBus
      s <- setup
      id <- commands.getAs[ID]("execute")
    } yield {
      val params = commands.getAs[SPAttributes]("parameters").getOrElse(SPAttributes())
      val resourceInfo = SPAttributes(
        "id" -> id, // should be the resource id here later
        "name" -> "plc"
      )
      val item = idMap.get(id).getOrElse(Operation("dummy"))
      val command = SPAttributes(
        "id" -> id,
        "name" -> item.name,
        "parameters" -> params.+(item.attributes.getAs[SPAttributes]("control").getOrElse(SPAttributes()))
      )

      // hardcoded db for testing! Will be filled later
      // DBValue(id: ID, value: SPValue, valueType: String, db: Int, offset: Int)
      val adr = AdressValues(109, 0, 1)
      val db = DBValue(id, toggle, "boolean", adr)

      val adr2 = AdressValues(109, 0, 0)
      val db2 = DBValue(id, !toggle, "boolean", adr2)

      val adr3 = AdressValues(109, 2, 0)
      val db3 = DBValue(id, 126, "int", adr3)

      toggle = !toggle

      val mess = SPAttributes(
        "resource"->resourceInfo,
        "commands"->List(command),
        "dbs"-> List(db, db2, db3)
      )

      bus ! SendMessage(Topic(s.publishTopic), AMQMessage(mess.toJson))
    }
  }

  def setupBus(s: BusSetup, rnr: RequestNReply) = {
    if (setup.isEmpty && !s.onlyAskStatus){
      setup = Some(s)
      serviceName = Some(rnr.req.service)
      idMap = rnr.req.ids.map(x => x.id -> x).toMap
      state = setupState(rnr.req.ids)
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.busIP}:61616")
    }
  }

  def setupState(ids: List[IDAble]) = {
    val i: SPValue = SPValue("i")
    val ops = ids.filter(_.isInstanceOf[Operation]).map(x => x.id -> SPValue("i"))
    val things = ids.filter(_.isInstanceOf[Thing]).map(x => x.id -> SPValue("notSet"))

    State(ops.toMap ++ things.toMap)
  }

  def connectedAttribute = {
    if (setup.isEmpty)
      SPAttributes("theBus"->"Not connected")
    else if (theBus.isEmpty)
      SPAttributes("theBus"->"Connecting")
    else
      SPAttributes("theBus"->"Connected")
  }

  override def postStop() = {
    println("stänger")
    theBus.map(_ ! CloseConnection)
  }


}



