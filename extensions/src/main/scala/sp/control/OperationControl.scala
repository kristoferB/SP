package sp.control

import akka.actor._
import org.json4s.JsonAST.JInt

import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._
import sp.opcMilo._

import scala.util.Try


object OperationControl extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "url" -> KeyDefinition("String", List(), Some(""))
    ),
    "connection"->SPAttributes(
      "resources" -> KeyDefinition("Option[ID]", List(), None),
      "connectionDetails" -> KeyDefinition("Option[ID]", List(), None)
    ),
    "command" -> SPAttributes(
      "commandType"->KeyDefinition("String", List("connect", "disconnect", "status", "subscribe", "unsubscribe", "start", "stop", "raw", "reset"), Some("connect")),
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

case class BusSetup(url: String)

sealed class Address
case class PLCAddress(db: Int, byte: Int, bit: Int) extends Address
case class BusAddress(name: String) extends Address

case class DBValue(name: String, id: ID, value: SPValue, valueType: String, address: SPValue)
case class DBConnection(name: String, valueType: String, db: Int, byte: Int, bit: Int, intMap: Map[String, SPValue], id: ID, busAddress: String)


case class IDWithName(id: ID, name: String, value: SPValue)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OperationControl(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher
  val serviceID = ID.newID
  var client = new MiloOPCUAClient()
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  var state: State = State(Map())
  var stateWithName:List[IDWithName]= List()
  var idMap: Map[ID, IDAble] = Map()
  var addressToIDMap: Map[String, ID] = Map()
  var connectionMap: Map[ID, DBConnection] = Map()
  var resourceTree: List[ResourceInTree] = List()
  var abilityToRun: Map[ID,ID] = Map()
  var modeToAbility: Map[ID,ID] = Map()

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(OperationControl.transformTuple._1)
      val connection = transform(OperationControl.transformTuple._2)
      val commands = transform(OperationControl.transformTuple._3)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      serviceName = Some(service)

      // println("Commands: " + commands.getAs[String]("commandType"))

      def silent = SPAttributes("silent" -> true)

      commands.getAs[String]("commandType").get match {
        case "connect" =>
          setupConnection(s, rnr)
          setupConnectionDetails(connection, rnr)
          makeResourceTree(connection, ids)
          createRunAndModeMaps(ids)

        case "disconnect" =>
          if(client.isConnected) client.disconnect()
          replyTo ! Response(List(),connectedAttribute() merge silent, rnr.req.service, rnr.req.reqID)
          setup = None

        case "subscribe" =>
          subscribe()
        case "unsubscribe" =>
          unsubscribe()

        case "start" =>
          commands.getAs[ID]("execute").foreach { id =>
            sendStart(commands, id)
          }
        case "stop" =>
          commands.getAs[ID]("execute").foreach { id =>
            sendStop(commands, id)
          }
        case "status" =>
          eventHandler ! Response(List(), SPAttributes("state"->state, "resourceTree"-> resourceTree, "silent"->true), serviceName.get, serviceID)
        case "reset" =>
          eventHandler ! Response(List(), SPAttributes("reset"->true, "silent"->true), serviceName.get, serviceID)
        case _ =>
      }
      replyTo ! Response(List(), connectedAttribute() merge SPAttributes("silent"->true), service, serviceID)
    }

    case StateUpdate(activeState) =>
      // eh ! Response(List(), SPAttributes("state"->activeState) merge silent, serviceName, serviceID)

      activeState.foreach { case (address, value) =>
        val id = addressToIDMap(address)
        val stringRep: String = value.to[Int].map(_.toString).getOrElse(value.to[String].getOrElse(""))
        val updV = connectionMap.get(id).flatMap(x => x.intMap.get(stringRep)).getOrElse(value)

        // if this is a "mode" variable, map id to ability id instead
        modeToAbility.get(id) match {
          case Some(aid) => state = state add (aid -> updV)
          case None => state = state add (id -> updV)
        }
      }
      val dbs = List()

      stateWithName = state.state.flatMap{case (id, value) =>
        val item = idMap.get(id)
        item.map(i => IDWithName(id, i.name, value))
      }.toList

      eventHandler ! Response(List(), SPAttributes("state"->state, "stateWithName"->stateWithName, "dbs"-> dbs,"resourceTree"-> resourceTree, "silent"->true), serviceName.get, serviceID)

    case x => {
      // println("PLC control got message "+x)
      //sender() ! SPError("What do you want me to do? "+ x)
    }
  }

  def setupConnection(s: BusSetup, rnr: RequestNReply) = {
    setup = Some(s)
    serviceName = Some(rnr.req.service)
    idMap = rnr.req.ids.map(x => x.id -> x).toMap
    println(s"connecting: $s")
    if(client.isConnected)
      eventHandler ! Response(List(), connectedAttribute, rnr.req.service, rnr.req.reqID)
    else {
      if(!client.connect("opc.tcp://192.168.0.10:4840")) {
        eventHandler ! SPError("Could not connect to server")
      } else {
        eventHandler ! Response(List(),connectedAttribute, rnr.req.service, rnr.req.reqID)
      }
    }
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
    else if (!client.isConnected)
      SPAttributes("theBus"->"Connecting")
    else
      SPAttributes("theBus"->"Connected")
  }

  def setupConnectionDetails(connection: SPAttributes, rnr: RequestNReply) = {
    val ids = rnr.req.ids
    for {
      info <- ids.find(_.name == "PLCConnection")
      list <- info.attributes.getAs[List[DBConnection]]("connection")
    } yield {
      connectionMap = list.map(db => db.id->db).toMap
      addressToIDMap = list.map(db => db.busAddress -> db.id).toMap
    }
  }

  def createRunAndModeMaps(ids: List[IDAble])  = {
    // TODO: HACK! Use the hierarchy...
    val ops = ids.filter(_.isInstanceOf[Operation])
    val abilities = ops.filter(_ match {case o: Operation => o.attributes.getAs[String]("operationType").getOrElse("")=="ability"})
    val things = ids.filter(_.isInstanceOf[Thing])

    abilityToRun = (for {
      a <- abilities
      r <- things.find(t=>a.name+".run"==t.name)
    } yield {
      (a.id -> r.id)
    }).toMap
    modeToAbility = (for {
      a <- abilities
      m <- things.find(t=>a.name+".mode"==t.name)
    } yield {
      (m.id -> a.id)
    }).toMap
  }

  def findConnectionDetails(list: List[IDAble]) = {
    list.find{i => i.attributes.getAs[String]("specification").contains("PLCConnection")}.map(_.id)
  }

  def subscribe() = {
    val nodes = connectionMap.values.map(_.busAddress).toList
    client.subscribeToNodes(nodes, self)
  }

  def unsubscribe() = {

  }

  def sendStart(commands: SPAttributes, id: ID) = {
    val params = commands.getAs[State]("parameters").getOrElse(State(Map()))
    val paramsString = commands.getAs[String]("parameters")

    val resourceInfo = SPAttributes("id" -> ID.newID, "name" -> "plc") // will later be used to match receiver
    val item = idMap.getOrElse(id, Operation("dummy"))

    val paramDB = params.state.flatMap{case (id, value) =>
      connectionMap.get(id).map(x => DBValue(x.name, x.id, value, x.valueType,
        if(x.busAddress == "") SPValue(PLCAddress(x.db, x.byte, x.bit))
        else SPValue(BusAddress(x.busAddress))))
    }.toList
    val paramFromString = paramsString.flatMap(getDBFromString)
    val paramToWrite = params.state.flatMap{case (id, value) =>
      idMap.get(id).map(item => SPAttributes("id"->id, "name"->item.name, "value"->value))
    }
    // HACK
    val rid = abilityToRun.get(id).getOrElse(ID.newID)
    // flip RUN and write it to PLC
    val runState = true // state.get(rid).flatMap(_.to[Boolean]).map(!_).getOrElse(false)
                        // println(s"the new state of run: $runState")

    println("***********************   SSENDiNG start" + SPValue(runState))
    val oDB = connectionMap.get(rid).map{db => DBValue(item.name, item.id, SPValue(runState), db.valueType,
      if(db.busAddress == "") SPValue(PLCAddress(db.db, db.byte, db.bit)) else SPValue(BusAddress(db.busAddress)))}

    val dbs = paramDB ++ List(paramFromString, oDB).flatten

    dbs.foreach { db =>
      println(db.name + " " + db.value + " " + db.address)
      db.address.to[BusAddress] match {
        case Some(a) => client.write(a.name, db.value)
        case _ =>
      }
//      dispenser.dispense JBool(true) JObject(List((name,JString(|var|CODESYS Control for Raspberry Pi SL.Application.IOs.conv1InputOp_run))))
    }
  }

  def sendStop(commands: SPAttributes, id: ID) = {
    val resourceInfo = SPAttributes("id" -> ID.newID, "name" -> "plc") // will later be used to match receiver
    val item = idMap.getOrElse(id, Operation("dummy"))

    val rid = abilityToRun.get(id).getOrElse(ID.newID)
    val runState = false // state.get(rid).flatMap(_.to[Boolean]).map(!_).getOrElse(false)

    val oDB = connectionMap.get(rid).map{db => DBValue(item.name, item.id, SPValue(runState), db.valueType,
      if(db.busAddress == "") SPValue(PLCAddress(db.db, db.byte, db.bit)) else SPValue(BusAddress(db.busAddress)))}

    val command = SPAttributes(
      "id" -> id,
      "name" -> item.name,
      "parameters" -> List()
    )
    val dbs = List(oDB)

    val mess = SPAttributes(
      "resource"->resourceInfo,
      "commands"->List(command),
      "command"->"write",
      "dbs"-> dbs
    )

    dbs.foreach {
      case Some(db) =>
        println(db.name + " " + db.value + " " + db.address)
        db.address.to[BusAddress] match {
          case Some(a) => client.write(a.name, db.value)
          case _ =>
        }
      case _ =>
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
      val vt = if (value.isInstanceOf[JInt]) "int" else "bool"
      DBValue("raw", ID.newID, value, vt, SPValue(PLCAddress(db, byte, bit)))
    }
    res.toOption
  }

  def makeResourceTree(connection: SPAttributes, ids: List[IDAble]) = {
    val tree = ids.find(_.name == "Resources")
    //val tree = connection.getAs[ID]("resources").flatMap(idMap.get)

    Try(tree.get.asInstanceOf[HierarchyRoot]).foreach{t =>
      val nameMap = t.getAllIDs.map{id =>
        val n = idMap.get(id).map(_.name).getOrElse("")
        id -> n
      }.toMap
      val temp = t.children.map{r =>
        val abilities = r.children.filter(_.children.nonEmpty).map{ab =>
          val par = ab.children.map{p => ItemInTree(p.item, nameMap(p.item))}
          AbilityInTree(ab.item, nameMap(ab.item), par)
        }
        val st = r.children.filter(_.children.isEmpty).map{ab =>
          ItemInTree(ab.item, nameMap(ab.item))
        }
        ResourceInTree(r.item, nameMap(r.item), st, abilities)
      }
      this.resourceTree = temp
    }
  }

  def makeDummyState() = {
    this.idMap
  }

  override def postStop() = {
    client.disconnect()
  }
}

case class ItemInTree(id: ID, name: String)
case class AbilityInTree(id: ID, name: String, parameters: List[ItemInTree])
case class ResourceInTree(id: ID, name: String, state: List[ItemInTree], abilities: List[AbilityInTree])
