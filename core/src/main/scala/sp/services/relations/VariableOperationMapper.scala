package sp.services.relations

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._

import scala.util.Try


object VariableOperationMapper extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "visualization" // to organize in gui. maybe use "hide" to hide service in gui
    )
  )
  val transformation = List[TransformValue[_]]()
  def props = ServiceLauncher.props(Props(classOf[VariableOperationMapper]))
}

class VariableOperationMapper extends Actor with ServiceSupport {
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val ops = ids.collect{case x: Operation => x}
      val vars = ids.collect{case x: Thing => x}

      val startMaps = Mapsar(Map[ID, Set[SPValue]](),
        Map[ID, Set[SPValue]](),
        Map[ID, Set[ID]](),
        Map[ID, Set[ID]]())
      val varMaps = ops.foldLeft(startMaps)((maps, op) => extractVars(op, maps))

      val res = VariableOperationMapResult(makeVDM(varMaps), makeOVM(varMaps), makeVOM(varMaps))

      replyTo ! Response(List(), SPAttributes("variableOperationMap"->res), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
    }


  }

  var c = 0

  def extractVars(o: Operation,
                  m: Mapsar) = {

    var updGVars = Map[ID, Set[SPValue]]()
    var updAVars = Map[ID, Set[SPValue]]()

    def fromGuard(p: Proposition): Unit = {
      p match {
        case AND(xs) => xs.foreach(fromGuard)
        case OR(xs) => xs.foreach(fromGuard)
        case NOT(x) => fromGuard(x)
        case pe: PropositionEvaluator =>
          val xs = List(pe.left, pe.right)

          val ids = xs.collect{case x: SVIDEval => x}
          val values = xs.collect{case x: ValueHolder => x}.map(_.v)

          ids.foreach { x =>
            val updL = updGVars.getOrElse(x.id, Set()) ++ values
            updGVars = updGVars + (x.id -> updL)
          }
      }
    }
    def fromAction(a: Action) = {
      val value: Set[SPValue] = a.value match {
        case ValueHolder(v) => Set(v)
        case _ => Set()
      }
      val updL = updAVars.getOrElse(a.id, Set()) ++ value
      updAVars = updAVars + (a.id -> updL)
    }

    o.conditions.foreach {
        case PropositionCondition(guard, actions, _) =>
          fromGuard(guard)
          actions.map(fromAction)
    }


    val oGV = m.opGuardVar + (o.id -> updGVars.keySet)
    val oAV = m.opActVar + (o.id -> updAVars.keySet)

    val resGVars = m.gVars ++ updGVars.map{case (k, v) => k -> (m.gVars.getOrElse(k, Set()) ++ v)}
    val resAVars = m.aVars ++ updAVars.map{case (k, v) => k -> (m.aVars.getOrElse(k, Set()) ++ v)}

    println(s"$c g: $resGVars")
    println(s"$c a: $resAVars")
    println(s"$c oG: $oGV")
    println(s"$c oA: $oAV")
    c += 1



    Mapsar(resGVars, resAVars, oGV, oAV)
  }

  def makeVDM(m: Mapsar) = {
    val joined = m.gVars.keySet ++ m.aVars.keySet
    joined.map{id =>
      val g = m.gVars.getOrElse(id, Set())
      val a = m.aVars.getOrElse(id, Set())
      val d = g ++ a
      VariableDomainMap(id, d, g, a)
    }.toList
  }

  def makeOVM(m: Mapsar) = {
    val joined = m.opGuardVar.keySet ++ m.opActVar.keySet
    joined.map{id =>
      val g = m.opGuardVar.getOrElse(id, Set())
      val a = m.opActVar.getOrElse(id, Set())
      OperationVariableMap(id, g, a)
    }.toList
  }

  def makeVOM(m: Mapsar) = {
    val joined = (m.opGuardVar.values.toList ++ m.opActVar.values.toList).toSet.flatten
    joined.map{id =>
      val g = m.opGuardVar.filter{case (op, xs) => xs.contains(id)}.keySet
      val a = m.opActVar.filter{case (op, xs) => xs.contains(id)}.keySet
      VariableOperationMap(id, g, a)
    }.toList
  }

}

case class Mapsar(gVars: Map[ID, Set[SPValue]],
                  aVars: Map[ID, Set[SPValue]],
                  opGuardVar: Map[ID, Set[ID]],
                  opActVar: Map[ID, Set[ID]])

case class VariableDomainMap(id: ID,
                             domain: Set[SPValue],
                             inGuards: Set[SPValue],
                             inActions: Set[SPValue])

case class OperationVariableMap(id: ID,
                                guardVars: Set[ID],
                                actionsVar: Set[ID])

case class VariableOperationMap(id: ID,
                                guardOps: Set[ID],
                                actionsOps: Set[ID])

case class VariableOperationMapResult(variableDomain: List[VariableDomainMap],
                                      operationMap: List[OperationVariableMap],
                                      variableMap: List[VariableOperationMap])
