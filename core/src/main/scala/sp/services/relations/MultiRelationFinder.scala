package sp.services.relations

import akka.actor._
import sp.domain._
import sp.domain.Logic._
import sp.system._
import sp.system.messages._
import scala.concurrent._
import scala.concurrent.duration._

object MultiRelationFinder extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "Relations"),
    "setup" -> SPAttributes(
      "twoOrThreeStates" -> KeyDefinition("String", List("two", "three"), Some("three")),
      "simpleThreeState" -> KeyDefinition("Boolean", List(), Some(true)),
      "iterations" -> KeyDefinition("Int", List(), Some(100)),
      "maxDepth" -> KeyDefinition("Int", List(), Some(-1)),
      "maxResets" -> KeyDefinition("Int", List(), Some(0)),
      "breakOnSameState" -> KeyDefinition("Boolean", List(), Some(true))),
    "input" -> SPAttributes(
      "operations" -> KeyDefinition("List[ID]", List(), Some(org.json4s.JArray(List()))),
      "groups" -> KeyDefinition("List[String]", List(), Some(org.json4s.JArray(List())))))
      // "initState" -> KeyDefinition("State", List(), None),
      // "goal" -> KeyDefinition("Proposition", List(), None)))

  val transformTuple = (
    TransformValue("setup", _.getAs[Setup]("setup")),
    TransformValue("input", _.getAs[Input]("input")))

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props = ServiceLauncher.props(Props(classOf[MultiRelationFinder]))
  // def props = ServiceLauncher.Props(classOf[MultiRelationFinder])
}

case class Setup(twoOrThreeStates: String, simpleThreeState: Boolean, iterations: Int, maxDepth: Int, maxResets: Int, breakOnSameState: Boolean)
case class Input(operations: List[ID], groups: List[String])

case class ItemRelation(id: ID, state: SPValue, relations: Map[ID, Set[SPValue]])

/**
 * Created by kristofer on 15-06-22.
 */
class MultiRelationFinder() extends Actor with sp.system.ServiceSupport with MultiRelationFinderLogic with calcMethods {

  import context.dispatcher
  implicit val timeout = akka.util.Timeout(2 seconds)

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      val setup = transform(MultiRelationFinder.transformTuple._1)
      val inpu = transform(MultiRelationFinder.transformTuple._2)

      // val condFromSpecsF = askAService(Request(condFromSpecsService, SPAttributes(), ids.filter(_.isInstanceOf[Specification])), serviceHandler)
      // val flattenOpsF = askAService(Request(flattenOperationService, SPAttributes("someInput"->"yes"), ids), serviceHandler)

      // val res: Future[Response] = for {
      //   condFromSpec <- condFromSpecsF
      //   flattenOps <- flattenOpsF
      // } yield {
      //   progress ! SPAttributes("status"-> "Received initial answer from external services")

      //   Response(List(), SPAttributes(), r.service, r.reqID)
      // }

      val o11 = Operation("o11", List(), SPAttributes("time" -> 2))
      val o12 = Operation("o12", List(), SPAttributes("time" -> 5))
      val o13 = Operation("o13", List(), SPAttributes("time" -> 4))
      val o21 = Operation("o21", List(), SPAttributes("time" -> 1.5))
      val o22 = Operation("o22", List(), SPAttributes("time" -> 2))
      val o23 = Operation("o23", List(), SPAttributes("time" -> 4.23))
      val ops = List(o11, o12, o13, o21, o22, o23)

      val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23))
      val sopArbi = SOP(Arbitrary(o12, o22))

      val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(List(sopSeq, sopArbi), "traj")
      val opsUpd = ops.map { o =>
        val cond = conditions.get(o.id).map(List(_)).getOrElse(List())
        o.copy(conditions = cond)
      }

      // lazy val ops = ids.filter(o =>
      //   o.isInstanceOf[Operation] &&
      //     o.attributes.getAs[TimeUnit]("time").isDefined).map(_.asInstanceOf[Operation])

      def mapOps[T](t: T) = opsUpd.map(o => o.id -> t).toMap

      lazy val initState = State(mapOps(OperationState.init))
      // lazy val operationRelationMap = ops.map(o => o -> ItemRelation(o.id, OperationState.init, Map())).toMap
      lazy val evalualteProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)
      lazy val opNameMap = opsUpd.map(o=> o.id -> o.name).toMap
      //Calculation -------------------------------------------------------
      progress ! SPAttributes("progress" -> "Iterate sequences")
      // val result = for {
      //   n <- 0 to setup.iterations
      //   straightSeq = findStraightSeq(opsUpd, initState, Map(), evalualteProp2)
      // } yield {
      //   println("Multi relation:")
      //   val toPrint = straightSeq.map{
      //     case(o,ir) => s"Op: ${o.name} ${ir.relations.map{case(id,value)=> s"${opNameMap(id)} $value"}}"
      //   }
      //   println(toPrint.mkString("\n"))
      // }

      // Future Ideas
      // case class OpsSomething(op: ID, state: SPValue)
      // case class OperationRelation(op1: OpSomething, op2: OpSomething, sop: SOP)
      // case class OpsRelMap(map: Map[Set[ID], Set[OperationRelation]])


      val result = (0 to setup.iterations).foldLeft((Set(),Map()):(Set[Seq[Operation]],Map[Operation,ItemRelation])){
        case((accOpseq,accOrm),n) => 
        val opSeqMap = findStraightSeq(opsUpd,initState,accOrm,evalualteProp2)
        (Set(opSeqMap.opSeq) ++ accOpseq, opSeqMap.orm)
      }
      // println("Multi relation:")
      //   val toPrint = result._2.map{
      //     case(o,ir) => s"Op: ${o.name} ${ir.relations.map{case(id,value)=> s"${opNameMap(id)} $value"}}"
      //   }
      //   println(toPrint.mkString("\n"))
      //   println(result._1)

      // import scala.util.{ Success, Failure }
      // result onComplete {
      //   case Success(response) => {
      //     replyTo ! response
      //     self ! PoisonPill
      //   }
      //   case Failure(e) => {
      //     // Probably already sent error
      //     self ! PoisonPill
      //   }
      // }

    }

  }

  //  def request(attr: ServiceInput, ids: List[IDAble]): Response = {
  //    val setup = attr._1
  //    val input = attr._2
  //    val condFromSpecsF = askAService(Request(condFromSpecsService, SPAttributes(), ids.filter(_.isInstanceOf[Specification])), serviceHandler)
  //    val flattenOpsF = askAService(Request(flattenOperationService, SPAttributes("someInput"->"yes"), ids), serviceHandler)
  //    val f = for {cond <- condFromSpecsF; ops <- flattenOpsF} yield {
  //      updateProgress(SPAttributes("status"-> "Received initial answer from external services"))
  //
  //      // identify relations
  //
  //      //loop
  //        // find a sequence
  //        // identify enabled and arbitrary
  //        // update relations
  //        // handle deadlocks
  //      // end loop
  //
  //
  //
  //      Response(List(), SPAttributes())
  //    }
  //    Await.result(f, 3 seconds)
  //  }

}
case class OperationSequenceAndMap(opSeq: Seq[Operation],orm: Map[Operation, ItemRelation])
sealed trait calcMethods {
  def findStraightSeq(ops: List[Operation], initState: State, operationRelationMap: Map[Operation, ItemRelation], evalSetup: EvaluateProp): OperationSequenceAndMap = {
    implicit val es = evalSetup

    def getEnabledOperations(state: State) = ops.filter(_.eval(state))

    def iterate(currentState: State, orm: Map[Operation, ItemRelation],opSeq: Seq[Operation]=Seq()): OperationSequenceAndMap = {
      lazy val enabledOps = getEnabledOperations(currentState)
      if (enabledOps.isEmpty) {
        OperationSequenceAndMap(opSeq.reverse,orm)
      } else {
        val res = enabledOps.map {
          o =>
            val ir = orm.getOrElse(o, ItemRelation(o.id, OperationState.init, Map()))

            val newMap = currentState.state.map {
              case (id, value) =>
                id -> (ir.relations.getOrElse(id, Set()) ++ Set(value))
            }
            o -> ir.copy(relations = newMap)

        }
        import scala.util.Random
        lazy val selectedOp = Random.shuffle(enabledOps).head

        iterate(selectedOp.next(currentState), orm ++ res,selectedOp+:opSeq)
      }
      // }
    }

    //Method starts
    iterate(initState, operationRelationMap)
  }

}
trait MultiRelationFinderLogic {

  def findOneSequence(ops: List[Operation], init: State, goal: State => Boolean) = {

  }

  def eval(o: Operation, s: State) = {
    //o.conditions.
  }

}
