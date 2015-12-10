package sp.opcrunner

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

case class SimulationSetup(command: String, state: State, operation : ID)

object Simulation extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Run operations based on their conditions and state"
    ),
    "setup" -> SPAttributes(
      "command" -> KeyDefinition("String", List("get init state", "execute"), Some("get init state")),
      "state" -> KeyDefinition("State", List(), Some(SPValue(State(Map())))),
      "operation" -> KeyDefinition("ID", List(), Some(SPValue(ID.newID)))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[SimulationSetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[Simulation]))
}

class Simulation extends Actor with ServiceSupport with DESModelingSupport {
  implicit val timeout = Timeout(100 seconds)

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      progress ! SPAttributes("progress" -> "starting simulation")

      println(r.attributes)

      val setup = transform(Simulation.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val things = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing])

      // TODO: clean up this mess
      def createOpsStateVars(ops: List[Operation]) = {
        ops.map(o => o.id -> sp.domain.logic.OperationLogic.OperationState.inDomain).toMap
      }
      val statevars = things.map(sv => sv.id -> sv.inDomain).toMap ++ createOpsStateVars(ops)
      implicit val props = EvaluateProp(statevars, Set(), ThreeStateDefinitionWithReset)

      setup.command match {
        case "get init state" =>
          val idleState = getIdleState(things.toSet)
          val initState = idleState match {
            case State(map) => State(map ++ ops.map(_.id -> OperationState.init).toMap)
          }
          println("initial state: " + initState)
          val enabledOps = ops.filter(_.conditions.filter(_.attributes.getAs[String]("kind").getOrElse("") == "precondition").headOption
            match {
              case Some(cond) => cond.eval(initState)
              case None => true
            })
          println("enabled operations: " + enabledOps)
          replyTo ! Response(List(), SPAttributes("simluation" -> "get init state",
            "newstate" -> initState, "enabled" -> enabledOps.map(_.id)),
            rnr.req.service, rnr.req.reqID)
          terminate(progress)

        case "execute" =>
          val state = setup.state
          val op = ops.find(_.id == setup.operation).get

          println("state before execute: " + state)
          println("op to execute: " + op)

          // todo: check if op is enabled
          val newState = op.next(state)
          val enabledOpsNow = ops.filter(_.conditions.filter(_.attributes.getAs[String]("kind").getOrElse("") == "precondition").headOption
            match {
              case Some(cond) => cond.eval(newState)
              case None => true
            })
          replyTo ! Response(List(), SPAttributes("simluation" -> "execute",
            "newstate" -> newState, "enabled" -> enabledOpsNow.map(_.id)),
            rnr.req.service, rnr.req.reqID)

          terminate(progress)
        case _ =>
          replyTo ! SPError("No such command")
          terminate(progress)
      }
    }
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
