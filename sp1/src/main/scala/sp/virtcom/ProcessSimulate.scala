package sp.virtcom

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import sp.services.AddHierarchies
import org.json4s.JString

import scala.util._

object ProcessSimulate extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Pull/push data from/to Process Simulate"
    ),
    "setup" -> SPAttributes(
      "ip" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "topic" -> KeyDefinition("String", List(), Some("PS"))
    ),
    "command" -> SPAttributes(
      "type" -> KeyDefinition("String", List("connect","disconnect",
        "export seq", "import basic ops", "import all", "import single", "update sim ids","import hierarchy roots",
        "get_operation_hierarchy"), Some("export seq")),
      "sops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List()))),
      "txid" -> KeyDefinition("String", List(), Some(""))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(modelHandler: ActorRef, eventHandler: ActorRef) = Props(classOf[ProcessSimulate], modelHandler, eventHandler)
}

case class BusSetup(ip: String, topic: String)

class ProcessSimulate(modelHandler: ActorRef, eventHandler: ActorRef) extends Actor with ServiceSupport with AddHierarchies {
  implicit val timeout = Timeout(100.seconds)
  val serviceID = ID.newID
  var bus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val setup = transform(ProcessSimulate.transformTuple._1)
      val command = transform(ProcessSimulate.transformTuple._2)

      for {
        c <- r.attributes.getAs[ServiceHandlerAttributes]("core")
        m <- c.model
      } yield {
        command.getAs[String]("type").get match {
          case "connect" => connect(setup)
          case "disconnect" => disconnect
          case "export seq" => exportSequence(m, ids, command.getAs[List[ID]]("sops").getOrElse(List()))
          case "import basic ops" => getBasicOps(m, ids)
          case "import all" => getAllItems(m, ids)
          case "import single" => getSingleItem(m, ids, command.getAs[String]("txid").getOrElse(""))
          case "update sim ids" => updateSimIds(m, ids)
          case "import hierarchy roots" =>
            val message = SPAttributes("command" -> "get_operation_roots")
            askPS(m, message, "import hierarchy roots")
          case "get_operation_hierarchy" =>
            val txid = command.getAs[String]("txid").getOrElse("");
            val message = SPAttributes("command" -> "get_operation_hierarchy", "txid" -> txid)
            askPS(m, message, "get operation hierarchy")
          case _ =>
        }
      }

      replyTo ! Response(List(), connectedAttribute merge SPAttributes("silent"->true), service, serviceID)
    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.foreach{ s=>
        c ! ConsumeFromTopic(s.topic)
        bus = Some(c)
        eventHandler ! Progress(connectedAttribute, serviceName.get, serviceID)
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }

    case _ => sender ! SPError("Ill formed request");
  }

  def connect(s: BusSetup)(implicit rnr: RequestNReply) = {
    if(bus.isEmpty) {
      setup = Some(s)
      serviceName = Some(rnr.req.service)
      println(s"ProcessSimulate: connecting to bus: $setup")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.ip}:61616")
    }
  }

  def connectedAttribute = {
    if (setup.isEmpty)
      SPAttributes("bus"->"Not connected")
    else if (bus.isEmpty)
      SPAttributes("bus"->"Connecting")
    else
      SPAttributes("bus"->"Connected")
  }

  def disconnect = {
    println("ProcessSimulate: disconnecting from the bus.")
    bus.foreach(_ ! CloseConnection)
    setup = None
    bus = None
    serviceName = None
  }

  override def postStop() = {
    disconnect
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }

  def createExportJsonHelper(sop : SOP, ids: List[IDAble], ack : SPAttributes=SPAttributes()) : SPAttributes = {
    sop match {
      case h: Hierarchy =>
        val ops = ids.find(o => o.id == h.operation)
        ack merge (ops match {
          case Some(o) => SPAttributes("name" -> o.name, "simop" -> (o.attributes.getAs[String]("simop") match {
            case Some(txid) => txid
            case _ =>
              o.attributes.getAs[String]("txid") match {
                case Some(txid) => txid
                case _ => "dummy"
              }
          }), "needsToBeCompleted" -> h.conditions.flatMap({
            case PropositionCondition(AND(x),_,_) =>
              x.flatMap({
                case EQ(SVIDEval(otherOpId), ValueHolder(JString("f"))) =>
                  // for now, only handle when the operation is finished
                  ids.find(o => o.id == otherOpId).map(_.name)
                case _ => None
              })
            case _ => None
          }))
          case None => SPAttributes("name" -> "op does not exist")
        })
      case p: Parallel => {
        ack merge SPAttributes("parallel" -> p.sop.map(createExportJsonHelper(_, ids, ack)))
      }
      case s: Sequence => {
        ack merge SPAttributes("sequence" -> s.sop.map(createExportJsonHelper(_, ids, ack)))
      }
    }
  }

  def exportSequence(model: ID, ids: List[IDAble], sops: List[ID])(implicit rnr: RequestNReply) = {
    val sopspecs = ids.flatMap { case s: SOPSpec if sops.contains(s.id) => Some(s); case _ => None }

    val message = SPAttributes("command" -> "create_op_chains",
      "params" -> SPAttributes("op_chains" -> sopspecs.map { spec =>
        val sop = spec.sop.map(createExportJsonHelper(_, ids))
        SPAttributes("name" -> spec.name, "sop" -> sop)
      }))

    askPS(model, message, "create_op_chains")
  }

  def addHierarchy(ids : List[IDAble], s : String) = {
    val uids = ids.map(x => x match { // TODO: real ugly...
      case i:Operation => i.copy(attributes = i.attributes merge SPAttributes("hierarchy" -> Set(s)))
      case i:Thing => i.copy(attributes = i.attributes merge SPAttributes("hierarchy" -> Set(s)))
      case i:SOPSpec => i.copy(attributes = i.attributes merge SPAttributes("hierarchy" -> Set(s)))
    })
    ids ++ addHierarchies(uids, "hierarchy")
  }

  def getBasicOps(model: ID, ids: List[IDAble]) = {
    val message = SPAttributes("command" -> "get_tx_basic_ops")
    askPS(model, message, "get_tx_basic_ops", addHierarchy(_, "PS Basic Ops"))
  }

  def getAllItems(model: ID, ids: List[IDAble]) = {
    val message = SPAttributes("command" -> "get_all_tx_objects")
    askPS(model, message, "get_all_tx_objects", addHierarchy(_, "PS All Items"))
  }

  def getSingleItem(model: ID, ids: List[IDAble], txid: String) = {
    val message = SPAttributes("command" -> "get_tx_object", "params" -> Map("txid" -> txid))
    askPS(model, message, "get_tx_object")
  }

  def updateSimIds(model: ID, ids: List[IDAble]) = {
    val message = SPAttributes("command" -> "get_tx_basic_ops")
    val opsInSP = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
    askPS(model, message, "update_sim_ids", {
      idablesFromPS =>
      lazy val opsFromPS = idablesFromPS.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      lazy val opsFromPSName_Map = opsFromPS.map(o => o.name -> o).toMap
      lazy val opsInSPName_Map = opsInSP.map(o => o.name -> o).toMap
      lazy val updatedSPOps = opsInSPName_Map.flatMap { case (spOpName, spOp) =>
        for {
          psOp <- opsFromPSName_Map.get(spOpName)
          txidValue <- psOp.attributes.getAs[String]("txid")
        } yield {
          val ssValue = psOp.attributes.getAs[Option[String]]("starting_signal").getOrElse(None)
          val esValue = psOp.attributes.getAs[Option[String]]("ending_signal").getOrElse(None)
          val durValue = psOp.attributes.getAs[Double]("duration").getOrElse(0)
          val updatedAttr = (spOp.attributes transformField { case ("simop", _) => ("simop", SPValue(txidValue)) }).to[SPAttributes].getOrElse(spOp.attributes)
          val updatedAttr1 = (updatedAttr transformField { case ("txid", _) => ("txid", SPValue(txidValue)) }).to[SPAttributes].getOrElse(updatedAttr)
          val updatedAttr2 = (updatedAttr1 transformField { case ("starting_signal", _) => ("starting_signal", SPValue(ssValue)) }).to[SPAttributes].getOrElse(updatedAttr1)
          val updatedAttr3 = (updatedAttr2 transformField { case ("ending_signal", _) => ("ending_signal", SPValue(esValue)) }).to[SPAttributes].getOrElse(updatedAttr2)
          val updatedAttr4 = (updatedAttr3 transformField { case ("duration", _) => ("duration", SPValue(durValue)) }).to[SPAttributes].getOrElse(updatedAttr3)
          if(spOp.attributes.toString != updatedAttr4.toString) {
            println(spOp.name)
            println(updatedAttr4)
          }
          spOp.copy(attributes = updatedAttr4)
        }
      }
      println(s"Updated attributes for ${updatedSPOps.size} operations. ${updatedSPOps.map(_.name).mkString(", ")}")
      updatedSPOps.toList
    })
  }

  def getOpsWithHierarchy(attr: SPAttributes): Either[(List[IDAble],SPAttributes),String] = {
    def hiernode(idmap: Map[String, IDAble], node: SPAttributes): HierarchyNode = {
      val idable = node.getAs[IDAble]("idable").getOrElse(Thing("hiernode_error", attributes=node))
      val txid = idable.attributes.getAs[String]("txid").getOrElse("")
      val children = node.getAs[List[SPAttributes]]("children").getOrElse(List())
      HierarchyNode(idmap(txid).id, children.map(hiernode(idmap,_)))
    }
    
    def idables(node: SPAttributes): List[IDAble] = {
      val idable = node.getAs[IDAble]("idable").getOrElse(Thing("idable_error", attributes=node))
      val children = node.getAs[List[SPAttributes]]("children").getOrElse(List())
      idable :: children.map(idables(_)).flatten
    }
    attr.getAs[SPAttributes]("root") match {
      case Some(rootNode) =>
        val ids = idables(rootNode)
        val idMap = ids.map(i=>i.attributes.getAs[String]("txid").getOrElse("")->i).toMap
        val rootChildren = rootNode.getAs[List[SPAttributes]]("children").get.map(hiernode(idMap,_))
        val hierarchyRoot = HierarchyRoot(rootNode.getAs[String]("name").get, rootChildren)
        Left((hierarchyRoot :: ids),SPAttributes())
      case None => Right("no root node")
    }
  }

  def handlePSAnswer(f: Future[Any]): Future[Either[(List[IDAble],SPAttributes),String]] = {
    // finds error by exception, since then the future fails
    f.map { case AMQMessage(body, props, headers) =>
      val psres = SPAttributes.fromJson(body.toString).get
      // println(psres.pretty)
      val responseType = psres.getAs[String]("response_type")

      responseType match {
        case Some("simple_ok") => Left((List(),SPAttributes()))
        case Some("list_of_items") => Left((psres.getAs[List[IDAble]]("items").getOrElse(List()),SPAttributes()))
        case Some("hierarchy_roots") => Left((List(),psres merge SPAttributes("silent"->true)))
        case Some("hierarchy") => getOpsWithHierarchy(psres)
        case Some("item") => Left((List(psres.getAs[IDAble]("item")).flatMap(x=>x),SPAttributes()))
        case Some("error") => Right(psres.getAs[String]("error").getOrElse("No error string defined"))
        case Some(x) => Right(s"Unrecognized response $x")
        case None => Right("No response_type")
      }
      case x@_ => Right("Unexpected response type $s")
    }
  }

  def askPS(model: ID, message: SPAttributes, responseInfo: String, responseTransformation: List[IDAble]=>List[IDAble] = identity) = {
    eventHandler ! Progress(SPAttributes("progress" -> "Request sent to PS. Waiting for answer"),
      serviceName.get, serviceID)
    val json = message.pretty
    val f = for {
      b <- bus
      s <- setup
    } yield {
      b ? RequestMessage(Queue(s.topic), AMQMessage(json))
    }
    f.foreach(handlePSAnswer(_).foreach{
      case Left((l,a)) =>
        if(l.nonEmpty) modelHandler ! UpdateIDs(model, responseTransformation(l), SPAttributes("info"->responseInfo))
        eventHandler ! Response(List(), a merge SPAttributes("info" -> responseInfo), serviceName.get, serviceID)
      case Right(s) =>
        eventHandler ! Response(List(), SPAttributes("error" -> s, "info" -> responseInfo), serviceName.get, serviceID)
    })
  }
}
