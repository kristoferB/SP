package sp.runners

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._



import sp.runners.{API_OperationRunner => api}

class OperationRunner extends Actor with ActorLogging with OperationRunnerLogic {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("events", self)


  def receive = {
    case x: String =>




  }



  def matchRequests(mess: Try[SPMessage]) = {
    OperationRunnerComm.extractRequest(mess).map{ case (h, b) =>

//      b match {
//        case b: api.Setup =>
//        case api.SetState(id, s) =>
//        case api.TerminateRunner(id) =>
//        case api.GetState(id) =>
//        case b: api.GetRunners =>
//
//      }


    }
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
  case class Runner(setup: api.Setup, currentState: Map[ID, SPValue], abilities: Set[ID])
  var runners: Map[ID, Runner] = Map()
  var abilities: Set[ID] = Set()



  object OperationState {
    val init: SPValue = "i"
    val executing: SPValue = "e"
    val finished: SPValue = "f"
    val domain = Set(init, executing, finished)
  }



  def addRunner(setup: api.Setup) = {
    val abs = extractAbilities(setup.ops)
    val r = Runner(setup, setup.initialState, abs)
    runners += setup.runnerID -> r
  }

  def setRunnerState(id: ID, s: State) = {
    val r = runners.get(id).map(_.copy(currentState = s.state))
    r.foreach(x =>
      runners += id -> x
    )
    r
  }




  def runOp(o: Operation, s: State) = {
    if (isEnabled(o, s)){    // kanske skippa detta
      val opState = s(o.id)
      val filtered = filterConditions(o.conditions, Set("pre", "precondition"))
      val newState = filtered.foldLeft(s){(tempS, cond) => cond.next(tempS)}
      Some(newState.next(o.id -> OperationState.executing))
    } else None
  }

  def evaluateOps(ops: List[Operation], s: State) = {
    ops.filter(o => isEnabled(o, s))
  }

  def isEnabled(o: Operation, s: State) = {
    val oState = s.get(o.id).getOrElse(SPValue(""))
    val xs = filterConditions(o.conditions, Set("pre", "precondition"))
    xs.forall(_.eval(s)) && oState == OperationState.init
  }
  def filterConditions(conds: List[Condition], set: Set[String]) = {
    conds filter(c => {
      val res = c.attributes.getAs[String]("kind").getOrElse("")
      (set contains res) || set.isEmpty
    })
  }


  def extractAbilities(ops: Set[Operation]) = {
    for {
      o <- ops
      ab <- o.attributes.getAs[ID]("ability")
    } yield ab
  }

}