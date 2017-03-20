package sp.opcRunner

import org.json4s.JsonAST.{JValue,JBool,JInt,JString}
import akka.actor._
import sp.domain.logic._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import sp.supremicaStuff.auxiliary.DESModelingSupport
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import org.joda.time.DateTime
import scala.util.Random

// import sp.labkit.{OperationStarted,OperationFinished}





object RunnerRuntime {
  def props = Props(classOf[RunnerRuntime])
}

case class Init(ops: List[Operation], things: List[Thing])
case class SetAutostart(ops: List[ID])
case class StartOp(op: ID)
case class RunSop(sop: ID, ids: List[IDAble])
case class ForceFinishOp(op: ID)
case class StateChange(state: State, enabled: List[ID])

case class RunnerOpStarted(resource: String, name: String, time: DateTime)
case class RunnerOpFinished(resource: String, name: String, time: DateTime)

class RunnerRuntime extends Actor with DESModelingSupport {
  import context.dispatcher
  val id = ID.newID
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("RunnerRuntimeCommands", self)
  mediator ! Subscribe("sp1answers", self)

  var ops: List[Operation] = List()
  var things: List[Thing] = List()
  var autoStart: List[ID] = List()
  var state: State = State(Map())
  implicit var props = EvaluateProp(Map(), Set(), ThreeStateDefinitionWithReset)
  var opToAb: Map[ID, ID] = Map()
  var abToOp: Map[ID, ID] = Map()
  var ablist: List[(ID,String)] = List()

  def enabledOps =
    if(opToAb.isEmpty)
      List()
    else ops.filter(_.conditions.filter(
      _.attributes.getAs[String]("kind").getOrElse("") == "precondition").headOption
      match {
      case Some(cond) => cond.eval(state)
      case None => true
    })

  def started = ops.nonEmpty && state.state.nonEmpty

  def autostart = {
    // autostart a random op
    val canStart = enabledOps.map(_.id).filter(autoStart.contains(_))
    if(canStart.nonEmpty) {
      val toStart = canStart(Random.nextInt(canStart.size))
      self ! StartOp(toStart)
    }
  }

  def setupAbMaps(abs: List[(ID,String)]) = {
    ablist = abs
    opToAb = ops.flatMap { o =>
      for {
        abName <- o.attributes.getAs[String]("ability")
        ab <- abs.find(_._2 == abName)
      } yield {
        o.id -> ab._1
      }
    }.toMap
    abToOp = opToAb.map { case (o,a) => a -> o }.toMap
    println("abilityMap: " + opToAb.mkString("\n"))
    mediator ! Publish("OPCRunnerFrontend", StateChange(state, enabledOps.map(_.id)))
  }

  def finishAb(id: ID) = {
    abToOp.get(id) match {
      case Some(opid) =>
        val op = ops.find(_.id == opid).get
        println("Finishing: " + op.name)
        state = op.next(state)
        val robot = op.attributes.getAs[String]("robotSchedule").getOrElse("")
        // mediator ! Publish("frontend", OperationFinished(op.name, robot, "", "", org.joda.time.DateTime.now.toString))
        mediator ! Publish("OPCRunnerFrontend", StateChange(state, enabledOps.map(_.id)))
        autostart
      case None =>
        println("No operation for ability: " + id)
    }
  }

  def receive = {
    case a: SPAttributes => {
      if(a.getAs[String]("from").getOrElse("") == "AbilityHandler") {
        println("got response: " + a)
        val abs = a.getAs[List[(ID,String)]]("abilities").getOrElse(List())
        if(abs.nonEmpty) setupAbMaps(abs)

        a.getAs[ID]("finished").foreach(id => finishAb(id))
      }
    }

    case Init(os, ts) => {
      ops = os
      things = ts
      def createOpsStateVars(ops: List[Operation]) = {
        ops.map(o => o.id -> sp.domain.logic.OperationLogic.OperationState.inDomain).toMap
      }
      val statevars = things.map(sv => sv.id -> sv.inDomain).toMap ++ createOpsStateVars(ops)
      props = EvaluateProp(statevars, Set(), ThreeStateDefinitionWithReset)

      val idleState = getIdleState(things.toSet)
      val initState = idleState match {
        case State(map) => State(map ++ ops.map(_.id -> OperationState.init).toMap)
      }
      state = initState
      mediator ! Publish("OPCRunnerFrontend", StateChange(state, enabledOps.map(_.id)))

      val mess = SPAttributes("from" -> "RunnerRuntime", "command" -> "GetAbs")
      mediator ! Publish("sp1services", mess)
    }

    case RunSop(id, ids) if ablist.nonEmpty =>
      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val things = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing])
      val sops = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])

      println("Running sop: " + id)
      val s = sops.find(_.id == id).get
      println("SOP name: " + s.name)
      import sp.domain.logic.SOPLogic
      println("before extract")
      val conds = SOPLogic.extractOperationConditions(s.sop, "sop")
      println("after extract")
      val updOps = ops.flatMap { o =>
        for {
          cond <- conds.get(o.id)
        } yield {
          o.copy(conditions = List(cond))
        }
      }.toList

      val opabmap = updOps.flatMap { o =>
        for {
          abName <- o.attributes.getAs[String]("ability")
          ab <- ablist.find(_._2 == abName)
        } yield {
          o.id -> ab._1
        }
      }.toMap

      val mess = SPAttributes("from" -> "RunnerRuntime", "command" -> "StartSOP",
        "ops" -> updOps, "abmap" -> opabmap.toList, "initstate" -> List())

      println("Sending mess: " + mess)

      mediator ! Publish("sp1services", mess)

    case SetAutostart(ol) if started =>
      autoStart = ol
      autostart

    case StartOp(id: ID) if started => {
      val op = ops.find(_.id == id).get
      // are we even allowed to start?
      if(enabledOps.contains(op)) {
        // find ability
        opToAb.get(op.id) match {
          case Some(abid) =>
            // start ab
            // immediately change internal state
            println("Starting: " + op.name)
            state = op.next(state)

            val mess = SPAttributes("from" -> "RunnerRuntime", "command" -> "StartAb", "id" -> abid)
            mediator ! Publish("sp1services", mess)

            mediator ! Publish("OPCRunnerFrontend", StateChange(state, enabledOps.map(_.id)))
            // hack for labkit frontend!
            val robot = op.attributes.getAs[String]("robotSchedule").getOrElse("")
            // mediator ! Publish("frontend", OperationStarted(op.name, robot, "", "", org.joda.time.DateTime.now.toString))
            autostart
          case None =>
            println("ability not found for: " + op.name + "!")
        }
      } else {
        println("op not allowed to start: " + op.name + "!")
      }
    }
    case ForceFinishOp(id: ID) if started => {
      state.state.find{ case (k,v) => k == id && v == OperationState.executing } match {
        case Some(_) =>
          val op = ops.find(_.id == id).get
          state = op.next(state)
          mediator ! Publish("OPCRunnerFrontend", StateChange(state, enabledOps.map(_.id)))
          val robot = op.attributes.getAs[String]("robotSchedule").getOrElse("")
          // mediator ! Publish("frontend", OperationFinished(op.name, robot, "", "", org.joda.time.DateTime.now.toString))
        case None =>
          println("op not running...")
      }
    }
    case _ => sender ! SPError("Ill formed request");
  }
}
