package sp.psl

import akka.actor._
import sp.domain.logic.{PropositionParser, ActionParser, IDAbleLogic}
import sp.psl.runnerService.RunnerService
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._
import sp.domain.Operation
import com.typesafe.config._



object LabKitModel extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "models",
      "description" -> "Creates items describing the LabKit system"
    )
  )
  val transformation: List[TransformValue[_]] = List()
  def props = Props(classOf[LabKitModel])
  //def props(eventHandler: ActorRef, serviceHandler: ActorRef, runnerService: String) =
    //ServiceLauncher.props(Props(classOf[LabKitModel], eventHandler, serviceHandler, runnerService))
}

class LabKitModel extends Actor with ServiceSupport with TheLabKitModel {
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val items = getCompleteModel

      replyTo ! Response(items, SPAttributes("info"->"Items created from LabKitModel service"), rnr.req.service, rnr.req.reqID)
    }
  }
}

trait TheLabKitModel extends LabKitResources with LabKitDBConnector{
  def getCompleteModel: List[IDAble] = {

    val items = getResources
    val itemMap = items.map(x => x.name -> x.id).toMap

    // This info will later on be filled by a service on the bus
    val connectionList = getDBMap(itemMap)
    val connection = SPSpec("PLCConnection", SPAttributes(
      "connection"->connectionList,
      "specification"-> "PLCConnection"
    ))
    items :+ connection
  }


}

trait LabKitResources extends ModelMaking {
  def getResources = {
    val conv1 = makeResource (
      name = "conv1",
      state = List(),
      abilities = List(
        AbilityDefinition("input"),
        AbilityDefinition("process")
      )
    )
    val conv2 = makeResource (
      name = "conv2",
      state = List(),
      abilities = List(
        AbilityDefinition("move")
      )
    )

    val root = HierarchyRoot("Resources", List(conv1._1, conv2._1))
    conv1._2 ++ conv2._2 :+ root
  }
}

trait LabKitDBConnector {
  def db(items: Map[String, ID], name: String, valueType: String, db:Int, byte: Int, bit: Int, intMap: Map[Int, String] = Map(), busAddress: String = "") = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id, busAddress))
  }

  def getDBMap(itemMap: Map[String, ID]) = {
    val stateMap = Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")

    val codesysprefix = "|var|CODESYS Control for Raspberry Pi SL.Application.IOs."
    List(
      db(itemMap, "conv1.input.run", "bool", 0, 0, 0, Map(), codesysprefix+"conv1InputOp_run"),
      db(itemMap, "conv1.input.mode", "int", 0, 0, 0, stateMap, codesysprefix+"conv1InputOp_mode"),

      db(itemMap, "conv1.process.run", "bool", 0, 0, 0, Map(), codesysprefix+"conv1ProcessOp_run"),
      db(itemMap, "conv1.process.mode", "int", 0, 0, 0, stateMap, codesysprefix+"conv1ProcessOp_mode"),

      db(itemMap, "conv2.move.run", "bool", 0, 0, 0, Map(), codesysprefix+"conv2MoveOp_run"),
      db(itemMap, "conv2.move.mode", "int", 0, 0, 0, stateMap, codesysprefix+"conv2MoveOp_mode")

    ).flatten
  }
}
