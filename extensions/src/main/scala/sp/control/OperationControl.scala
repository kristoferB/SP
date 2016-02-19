package sp.control

import akka.actor._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import org.json4s.JsonAST.JInt
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._

import scala.util.Try


object OperationControl extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "busIP" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "publishTopic" -> KeyDefinition("String", List(), Some("commands")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("response"))
    ),
    "connection"->SPAttributes(
      "resources" -> KeyDefinition("Option[ID]", List(), None),
      "connectionDetails" -> KeyDefinition("Option[ID]", List(), None)
    ),
    "command" -> SPAttributes(
      "commandType"->KeyDefinition("String", List("connect", "disconnect", "status", "subscribe", "unsubscribe", "execute", "raw"), Some("connect")),
      "execute" -> KeyDefinition("Option[ID]", List(), None),
      "parameters" -> KeyDefinition("Option[State]", List(), None),
      "raw" -> KeyDefinition("String", List(), Some("")) // db byte bit value
    )
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("connection", _.getAs[SPAttributes]("connection")),
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[OperationControl], eventHandler)
}


case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)

case class AddressValues(db: Int, byte: Int, bit: Int)
case class DBValue(name: String, id: ID, value: SPValue, valueType: String, address: AddressValues)
case class DBConnection(name: String, valueType: String, db: Int, byte: Int, bit: Int, intMap: Map[Int, String], id: ID)


// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OperationControl(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher
  val serviceID = ID.newID

  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  var state: State = State(Map())
  var idMap: Map[ID, IDAble] = Map()
  var connectionMap: Map[ID, DBConnection] = Map()

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(OperationControl.transformTuple._1)
      val connection = transform(OperationControl.transformTuple._2)
      val commands = transform(OperationControl.transformTuple._3)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      commands.getAs[String]("commandType").get match {
        case "connect" =>
          setupBus(s, rnr)
          setupConnection(connection, rnr)
        case "disconnect" =>
          disconnect();
        case "subscribe" =>
          if (theBus.isEmpty)
            replyTo ! SPError("The bus must be connected before subscription. Current status: "+connectedAttribute())
          else
            subscribe()
        case "unsubscribe" =>
          if (theBus.isEmpty)
            replyTo ! SPError("The bus must be connected before unsubscription. Current status: "+connectedAttribute())
          else
            unsubscribe()
        case "execute" =>
          sendCommands(commands)
        case "raw" =>
          sendRaw(commands)
        case _ =>

      }

      replyTo ! Response(List(), connectedAttribute(), service, serviceID)

    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.foreach{ s=>
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

      for {
        m <- resp
        list <- m.getAs[List[SPAttributes]]("dbs")
      } yield for {
        l <- list
        id <- l.getAs[ID]("id")
        value <- l.getAs[SPValue]("value")
      } yield {
        state = state add (id -> value)
      }
      // fixa här
      eventHandler ! Response(List(), SPAttributes("resp"->resp, "state"->state), serviceName.get, serviceID)
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



  def setupBus(s: BusSetup, rnr: RequestNReply) = {
      setup = Some(s)
      serviceName = Some(rnr.req.service)
      idMap = rnr.req.ids.map(x => x.id -> x).toMap
      state = setupState(rnr.req.ids)
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.busIP}:61616")
  }

  def setupState(ids: List[IDAble]) = {
    val i: SPValue = SPValue("i")
    val ops = ids.filter(_.isInstanceOf[Operation]).map(x => x.id -> SPValue("notReady"))
    val things = ids.filter(_.isInstanceOf[Thing]).map(x => x.id -> SPValue("notSet"))

    State(ops.toMap ++ things.toMap)
  }

  def connectedAttribute() = {
    if (setup.isEmpty)
      SPAttributes("theBus"->"Not connected")
    else if (theBus.isEmpty)
      SPAttributes("theBus"->"Connecting")
    else
      SPAttributes("theBus"->"Connected")
  }

  def setupConnection(connection: SPAttributes, rnr: RequestNReply) = {
    for {
      id <- connection.getAs[ID]("connectionDetails")
      info <- idMap.get(id)
      list <- info.attributes.getAs[List[DBConnection]]("connection")
    } yield {
      connectionMap = list.map(db => db.id->db).toMap
    }
  }


  def disconnect() = {
    println("stänger")
    unsubscribe()
    theBus.foreach(_ ! CloseConnection)
    theBus.foreach(_ ! PoisonPill)
    this.setup = None
    this.theBus = None
  }


  def subscribe() = {
    val mess = SPAttributes(
      "command"->"subscribe",
      "dbs"-> this.connectionMap.values.map(x=>DBValue(x.name, x.id, 0, x.valueType, AddressValues(x.db, x.byte, x.bit)))
    )
    sendMessage(mess)
  }

  def unsubscribe() = {
    val mess = SPAttributes(
      "command"->"unsubscribe"
    )
    sendMessage(mess)
  }


  def sendCommands(commands: SPAttributes) = {
    commands.getAs[ID]("execute").foreach { id =>
      val params = commands.getAs[State]("parameters").getOrElse(State(Map()))
      val paramsString = commands.getAs[String]("parameters")

      val resourceInfo = SPAttributes("id" -> ID.newID, "name" -> "plc") // will later be used to match receiver
      val item = idMap.getOrElse(id, Operation("dummy"))

      val paramDB = params.state.flatMap{case (id, value) =>
        connectionMap.get(id).map(x => DBValue(x.name, x.id, value, x.valueType, AddressValues(x.db, x.byte, x.bit)))
      } toList
      val paramFromString = paramsString.flatMap(getDBFromString)

      val oDB = connectionMap.get(id).map{db => DBValue(item.name, item.id, true, "boolean", AddressValues(db.db, db.byte, db.bit))}

      val command = SPAttributes(
        "id" -> id,
        "name" -> item.name,
        "parameters" -> paramDB   // change this to name and id later
      )
      val dbs = paramDB ++ List(paramFromString, oDB).flatten

      val mess = SPAttributes(
        "resource"->resourceInfo,
        "commands"->List(command),
        "command"->"write",
        "dbs"-> (dbs)
      )

      sendMessage(mess)

    }
  }

  def sendRaw(commands: SPAttributes)= {
    commands.getAs[String]("raw").foreach{str=>
      val db = getDBFromString(str)
      val mess = SPAttributes(
        "command"->"write",
        "dbs"-> List(db).flatten
      )
      sendMessage(mess)
    }
  }

  def getDBFromString(s: String)= {
    val split = s.split("\\s+")
    val res = Try{
      val db = split(0).toInt
      val byte = split(1).toInt
      val bit = split(2).toInt
      val v = split(3)
      val value = if (Try{v.toInt}.isSuccess) SPValue(v.toInt) else if (Try{v.toBoolean}.isSuccess) SPValue(v.toBoolean) else SPValue(false)
      val vt = if (value.isInstanceOf[JInt]) "int" else "boolean"
      DBValue("raw", ID.newID, split(3), vt, AddressValues(db, byte, bit))
    }

    res.toOption
  }



  def sendMessage(mess: SPAttributes) = {
    for {
      bus <- theBus
      s <- setup
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.publishTopic), AMQMessage(mess.toJson))
    }
  }




  override def postStop() = {
    println("stänger")
    disconnect()
  }


}



