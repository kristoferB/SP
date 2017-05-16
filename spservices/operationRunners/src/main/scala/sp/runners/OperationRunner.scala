package sp.runners

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._
import sp.runners.APIAbilityHandler.AbilityStarted
import sp.runners.API_OperationRunner.ForceComplete
import sp.runners.{API_OperationRunner => api}
import sp.runners.{APIAbilityHandler => abilityAPI}


class OperationRunner extends Actor with ActorLogging with OperationRunnerLogic {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("events", self)


  def receive = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      matchRequests(mess)
      matchAbilityAPI(mess)
      matchServiceRequests(mess)
      // if needed, also get the state from the VD here


  }



  def matchRequests(mess: Try[SPMessage]) = {

    OperationRunnerComm.extractRequest(mess).map{ case (h, b) =>
      val updH = h.copy(from = api.attributes.service)
      mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, APISP.SPACK()))
      b match {
        case setup: api.Setup =>
          addRunner(setup).foreach{xs =>
            mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, api.Runners(xs)))

            val state = runners(setup.runnerID).currentState

            println("Runner started. Init state: " + state)
            setRunnerState(setup.runnerID, State(state), startAbility, sendState(_, setup.runnerID), false)

          }

        case api.SetState(id, s) =>
          setRunnerState(id, State(s), startAbility, sendState(_, id)) match {
            case Some(_) =>
            case None =>
              mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, APISP.SPError(s"no runner with id: $id")))
          }
        case api.AddOperations(id, ops, map) =>
          updRunner(id, ops, Set(), map, startAbility, sendState(_, id) )
        case api.RemoveOperations(id, ops) =>
          updRunner(id, Set(), ops, Map(), startAbility, sendState(_, id) )
        case api.TerminateRunner(id) =>
          val xs = removeRunner(id)
          mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, api.Runners(xs)))
        case api.GetState(id) =>
          getRunnerState(id) match {
            case Some(s) =>
              mediator ! Publish("services", OperationRunnerComm.makeMess(updH, api.StateEvent(id, s)))
            case None =>
              mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, APISP.SPError(s"no runner with id: $id")))
          }
        case b: api.GetRunners =>
          val xs = runners.map(_._2.setup).toList
          mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, api.Runners(xs)))
        case ForceComplete(id) =>
          completeOPs(id, startAbility, sendState)
       }

      mediator ! Publish("answers", OperationRunnerComm.makeMess(updH, APISP.SPDone()))


    }
  }


  def matchAbilityAPI(mess: Try[SPMessage]) = {
    OperationRunnerComm.extractAbilityReply(mess).map { case (h, b) =>

        b match {
          case abilityAPI.AbilityStarted(id) =>
            val ops = getOPFromAbility(id).flatMap(_._2)
            println(s"The ability with id $id started for operations: $ops")
          case abilityAPI.AbilityCompleted(id, _) =>
            val ops = getOPFromAbility(id).flatMap(_._2)
            println(s"The ability with id $id completed for operations: $ops")
            completeOPs(id, startAbility, sendState)
          case abilityAPI.AbilityState(id, s) =>

            //val ops = getOPFromAbility(id).flatMap(_._2)
            val abState = (for {
              x <- s.get(id) if x.isInstanceOf[SPAttributes]
              v <- x.asInstanceOf[SPAttributes].get("state")
            } yield v).getOrElse(SPValue("notEnabled"))
            println(s"The ability with id $id updated with state: $abState")
            newAbilityState(id, abState, startAbility, sendState)
        }

    }
  }

  def matchServiceRequests(mess: Try[SPMessage]) = {
    OperationRunnerComm.extractServiceRequest(mess) map { case (h, b) =>
      val spHeader = h.copy(from = api.attributes.service)
      mediator ! Publish("spevents", OperationRunnerComm.makeMess(spHeader, APISP.StatusResponse(statusResponse)))
    }
  }


  val startAbility = (id: ID) => {
    var i = 1
    for(i <- 1 to 10000){
      print("Delay")
    }
    println("Starting ability: " + id)
    val myH = SPHeader(from = api.attributes.service, to = abilityAPI.attributes.service, reply = api.attributes.service)
    mediator ! Publish("services", OperationRunnerComm.makeMess(myH, abilityAPI.StartAbility(id)))
  }

  val sendState = (s: State, id: ID) => {
    println(s"new state for $id: " +s)
    val myH = SPHeader(from = api.attributes.service)
    mediator ! Publish("events", OperationRunnerComm.makeMess(myH, api.StateEvent(id, s.state)))

  }




  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "tags" -> List("operation", "runtime"),
    "attributes" -> api.attributes,
    "instanceID" -> ID.newID
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    val mess = SPMessage.makeJson(SPHeader(from = api.attributes.service, to = "serviceHandler"), statusResponse)
    mediator ! Publish("spevents", mess)
  }


}

object OperationRunner {
  def props = Props(classOf[OperationRunner])
}





/*
 * Using a trait to make the logic testable
 */
trait OperationRunnerLogic {
  case class Runner(setup: api.Setup, currentState: Map[ID, SPValue]) {
    val noAbilityOps = setup.ops.filter(o => !setup.opAbilityMap.contains(o.id))
  }
  var runners: Map[ID, Runner] = Map()
  var abilities: Set[ID] = Set()



  object OperationState {
    val init: SPValue = "i"
    val executing: SPValue = "e"
    val finished: SPValue = "f"
    val domain = Set(init, executing, finished)
  }


  def updOPs(o: Operation, opAbilityMap: Map[ID, ID]) = {
    opAbilityMap.get(o.id).map{id =>
      val c = PropositionCondition(EQ(SVIDEval(id), ValueHolder("enabled")), List(), SPAttributes("kind" -> "pre"))
      o.copy(conditions = c :: o.conditions)
    }
  }

  def addRunner(setup: api.Setup) = {
    val updState = setup.initialState ++
      setup.ops.map(o => o.id -> SPValue(OperationState.init)) ++
      setup.opAbilityMap.values.toList.map(id => id -> SPValue("notEnabled"))
    val updOps = setup.ops.flatMap(o => updOPs(o, setup.opAbilityMap))
    val r = Runner(setup.copy(initialState = updState, ops = updOps), updState)
    if (! validateRunner(setup)) None
    else {
      runners += setup.runnerID -> r
      Some(runners.values.toList.map(_.setup))
    }
  }

  def updRunner(runner: ID,
                add: Set[Operation],
                remove: Set[ID],
                opAbilityMap: Map[ID, ID],
                startAbility: ID => Unit,
                sendState: State => Unit
               ) = {
    val updR = runners.get(runner).map {runner =>
      val updOps = (runner.setup.ops ++ add).filter(o => !remove.contains(o.id)).flatMap(o => updOPs(o, runner.setup.opAbilityMap))
      val updMap = (runner.setup.opAbilityMap ++ opAbilityMap).filter(kv => !remove.contains(kv._1))
      val updSetup = runner.setup.copy(ops = updOps, opAbilityMap = updMap)
      val updState = (runner.currentState ++ add.map(o => o.id -> SPValue(OperationState.init))).filter(kv => !remove.contains(kv._1)) ++
        runner.setup.opAbilityMap.values.toList.map(id => id -> SPValue("notEnabled"))

      Runner(updSetup, updState)
    }
    updR.foreach{r =>
      runners += runner -> r
      setRunnerState(runner, State(r.currentState), startAbility, sendState)
    }
  }

  private def validateRunner(setup: api.Setup) = {
    !runners.contains(setup.runnerID)
    //setup.ops.forall(o => setup.opAbilityMap.contains(o.id))
  }

  def completeOPs(ability: ID, startAbility: ID => Unit, sendState: (State, ID) => Unit, runOneAtTheTime: Boolean = false) = {
    val tempR = runners
    val ops = tempR.map{r =>
      val opsID = r._2.setup.opAbilityMap.filter(_._2 == ability).keySet
      val xs = r._2.setup.ops.filter(o => opsID.contains(o.id))
      xs.map{o =>
        val cS = State(runners(r._1).currentState)
        val s = completeOP(o, cS)
        setRunnerState(r._1, s, startAbility, sendState(_,r._1), runOneAtTheTime)
      }
      r._2.noAbilityOps.map {o =>
        val cS = State(runners(r._1).currentState)
        val s = completeOP(o, cS)
        setRunnerState(r._1, s, startAbility, sendState(_,r._1), runOneAtTheTime)
      }
    }
  }

  def newAbilityState(ability: ID, abilityState: SPValue, startAbility: ID => Unit, sendState: (State, ID) => Unit) = {
    runners.map{r =>
      if (r._2.setup.opAbilityMap.values.toSet.contains(ability)){
        println("An ability has an operation and was updated")
        val cS = State(runners(r._1).currentState + (ability -> abilityState))
        setRunnerState(r._1, cS, startAbility, sendState(_,r._1))
      }
    }
  }

  def setRunnerState(runnerID: ID, s: State, startAbility: ID => Unit, sendState: State => Unit, runOneAtTheTime: Boolean = false) = {
    val r = runners.get(runnerID)
    r.map { x =>
      println("set runner state from: " + x.currentState + " to " + s)
      if (s != x.currentState) sendState(s)
      val startOP = (o: Operation) => {
        x.setup.opAbilityMap.get(o.id).foreach(startAbility)
      }
      runners += runnerID -> x.copy(currentState = s.state)
      newState(s,x.setup.ops, startOP, sendState, runOneAtTheTime)
    }
  }

  def removeRunner(id: ID) = {
    runners = runners - id
    runners.values.toList.map(_.setup)
  }

  def getRunnerState(id: ID) = {
    runners.get(id).map(_.currentState)
  }






  def newState(s: State, ops: Set[Operation], sendCmd: Operation => Unit, sendState: State => Unit, runOneAtTheTime: Boolean = false): State = {
    val enabled = ops.filter(isEnabled(_, s))
    val tempR = runners
    val enAb = runners.map(r =>
       r._2.setup.opAbilityMap.filter(a => a.equals(SPValue("notEnabled")))).toList
    // kolla abilities. Jämföra och kolla vilka som är not enabled, kanske.
    println("runner enabled abilities: " + enAb.toString())
    println("runner enabled ops: " + enabled.map(_.name).mkString(", "))
    val res = enabled.headOption.map{o =>
      val updS = runOp(o, s)
      sendCmd(o)
      sendState(updS)
      if (runOneAtTheTime) updS else newState(updS, ops - o, sendCmd, sendState, false)
    }
    res.getOrElse(s)
  }


  def runOp(o: Operation, s: State) = {
      val filtered = filterConditions(o.conditions, Set("pre", "precondition"))
      val newState = filtered.foldLeft(s){(tempS, cond) => cond.next(tempS)}
      newState.next(o.id -> OperationState.executing)
  }

  def completeOP(o: Operation, s: State) = {
    val filtered = filterConditions(o.conditions, Set("post", "postcondition"))
    val newState = filtered.foldLeft(s){(tempS, cond) => cond.next(tempS)}
    newState.next(o.id -> OperationState.finished)
  }

  def evaluateOps(ops: List[Operation], s: State) = {
    ops.filter(o => isEnabled(o, s))
  }

  def isEnabled(o: Operation, s: State) = {
    val oState = s(o.id)
    val xs = filterConditions(o.conditions, Set("pre", "precondition"))
    xs.forall(_.eval(s)) && oState == OperationState.init
  }
  def filterConditions(conds: List[Condition], set: Set[String]) = {
    conds filter(c => {
      val res = c.attributes.getAs[String]("kind").getOrElse("")
      (set contains res) || set.isEmpty
    })
  }

  def getOPFromAbility(id: ID) = {
    runners.filter{r =>
      r._2.setup.opAbilityMap.values.toSet.contains(id) // maybe precalculate this
    }.map(kv => kv._1 -> kv._2.setup.opAbilityMap.foldLeft(Set[ID]())((a, b) => {
      if (b._2 == id) a + b._1 else a
    })).toList

  }


}
