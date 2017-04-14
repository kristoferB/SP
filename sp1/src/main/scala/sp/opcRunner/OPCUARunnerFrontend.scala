package sp.opcRunner

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import org.json4s.JsonAST.{JValue,JBool,JInt,JString}
import org.json4s.DefaultFormats
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import sp.system.SPActorSystem.system
import org.joda.time.DateTime

case class FrontendSetup(command: String, operation : ID, operations: List[ID])

object OPCUARunnerFrontend extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Run operations based on their conditions and state"
    ),
    "setup" -> SPAttributes(
      "command" -> KeyDefinition("String", List("get init state", "execute","re-filter enabled by state", "force finish", "set autostart", "run sop"), Some("get init state")),
      "operation" -> KeyDefinition("ID", List(), Some(SPValue(ID.newID))),
      "operations" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[FrontendSetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[OPCUARunnerFrontend], eventHandler)
}

class OPCUARunnerFrontend(eh: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(system).mediator

  val serviceID = ID.newID
  val serviceName = "OPCUARunnerFrontend"

  val silent = SPAttributes("silent" -> true)

  mediator ! Subscribe("OPCRunnerFrontend", self)

  def receive = {
    case r@Request(service, attr, ids, reqID) =>
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val setup = transform(OPCUARunnerFrontend.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val things = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing])
      val sops = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])

      setup.command match {
        case "get init state" =>
          mediator ! Publish("RunnerRuntimeCommands", Init(ops, things))

        case "execute" =>
          mediator ! Publish("RunnerRuntimeCommands", StartOp(setup.operation))

        case "force finish" =>
          mediator ! Publish("RunnerRuntimeCommands", ForceFinishOp(setup.operation))

        case "set autostart" =>
          mediator ! Publish("RunnerRuntimeCommands", SetAutostart(setup.operations))

        case "run sop" =>
          mediator ! Publish("RunnerRuntimeCommands", RunSop(setup.operation, ids))

        case _ =>
          println("doing nothing")

      }
      replyTo ! Response(List(), silent, serviceName, serviceID)

    case StateChange(newstate, enabled) =>
      eh ! Response(List(), SPAttributes("newstate"->newstate, "enabled" -> enabled) merge silent, serviceName, serviceID)

    case _ =>
      // sender ! SPError("Ill formed request");
  }
}
