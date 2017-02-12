package sp.services

import akka.actor._
import sp.domain._
import sp.system.messages._
import sp.domain.logic.PropositionParser
import sp.domain.logic.ActionParser
import sp.domain.Logic._
import sp.system._
import akka.util._
import scala.concurrent.duration._
import org.json4s._

/**
 * The service parse a string into a proposition and returns it.
 * Send a request including attributes:
 * "model" -> "the name of the model"
 * "command" -> parse
 * "toParse" -> "the string to parse" e.g. r1.tool==kalle AND r2 = false
 * If we need better performance from multiple requests in the future,
 * we can have multiple actors in a round robin.
  **/
object PropositionParserService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "Hidden",
      "description" -> "Parse and print proposition conditions"
    ),
    "command"->KeyDefinition("String", List("parseGuard", "printGuard", "parseAction", "printAction"), Some("parseGuard"))
  )


  val transformation = List()

  def props = ServiceLauncher.props(Props(classOf[PropositionParserService]))
}

class PropositionParserService extends Actor with ServiceSupport {
  implicit val timeout = Timeout(5 seconds)

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)
      progress ! SPAttributes("progress" -> "starting prop parser")

      val command = r.attributes.getAs[String]("command").get
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      command match {
        case "parseGuard" =>
          val toParse = r.attributes.getAs[String]("toParse").get
          PropositionParser(ids).parseStr(toParse) match {
            case Left(failure) =>
              val errorMess = "[" + failure.next.pos + "] error: " + failure.msg + "\n\n" + failure.next.pos.longString
              replyTo ! Response(List(), SPAttributes("parseError" -> errorMess), rnr.req.service, rnr.req.reqID)
            case Right(prop) => {
              replyTo ! Response(List(), SPAttributes("proposition" -> prop), rnr.req.service, rnr.req.reqID)
            }
          }
          terminate(progress)

        case "printGuard" =>
          val prop = r.attributes.getAs[Proposition]("toPrint").get
          val str = prettyPrint(ids)(prop)
          replyTo ! Response(List(), SPAttributes("print" -> str),
            rnr.req.service, rnr.req.reqID)
          terminate(progress)

        case "parseAction" =>
          val toParse = r.attributes.getAs[String]("toParse").get
          try { // TODO: make actionparser not throw...
            ActionParser(ids).parseStr(toParse) match {
              case Left(failure) =>
                val errorMess = "[" + failure.next.pos + "] error: " + failure.msg + "\n\n" + failure.next.pos.longString
                replyTo ! Response(List(), SPAttributes("parseError" -> errorMess), rnr.req.service, rnr.req.reqID)
              case Right(action) => {
                replyTo ! Response(List(), SPAttributes("action" -> action), rnr.req.service, rnr.req.reqID)
              }
            }
          } catch {
            case e:Throwable => replyTo ! Response(List(), SPAttributes("parseError" -> e.toString), rnr.req.service, rnr.req.reqID)
          }
          terminate(progress)

        case "printAction" =>
          val action = r.attributes.getAs[Action]("toPrint").get
          val str = prettyPrintAction(ids)(action)
          replyTo ! Response(List(), SPAttributes("print" -> str),
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

  def stateVal(x : StateEvaluator, ids: List[IDAble]): String = {
    x match {
      case ValueHolder(v: JValue) => v.extract[String]
      case SVIDEval(id: ID) => ids.find(x => x.id == id).map(_.name).getOrElse(id.toString)
      case _ => "not implemented"
    }
  }

  def prettyPrint(ids: List[IDAble])(prop: Proposition): String = {
    prop match {
      case AND(props: List[Proposition]) => props.map(prettyPrint(ids)).mkString(" && ")
      case OR(props: List[Proposition]) => props.map(prettyPrint(ids)).mkString(" || ")
      case EQ(left: StateEvaluator, right: StateEvaluator) => stateVal(left,ids) + " == " + stateVal(right,ids)
      case NEQ(left: StateEvaluator, right: StateEvaluator) => stateVal(left,ids) + " != " + stateVal(right,ids)
      case GREQ(left: StateEvaluator, right: StateEvaluator) => stateVal(left,ids) + " >= " + stateVal(right,ids)
      case LEEQ(left: StateEvaluator, right: StateEvaluator) => stateVal(left,ids) + " <= " + stateVal(right,ids)
      case GR(left: StateEvaluator, right: StateEvaluator) => stateVal(left,ids) + " > " + stateVal(right,ids)
      case LE(left: StateEvaluator, right: StateEvaluator) => stateVal(left,ids) + " < " + stateVal(right,ids)
      case AlwaysTrue => "true"
      case AlwaysFalse => "false"
      case s@_ => "i forgot something: " + s.toString
    }
  }

  def prettyPrintAction(ids: List[IDAble])(action: Action): String = {
    val id = ids.find(x => x.id == action.id).map(_.name).getOrElse(action.id.toString)
    action.value match {
      case INCR(1) => id + "++"
      case INCR(n) => id + " += " + n.toString
      case DECR(1) => id + "--"
      case DECR(n) => id + " -= " + n.toString
      case ASSIGN(rhs: ID) => id + " := " + ids.find(x => x.id == rhs).map(_.name).getOrElse(rhs.toString)
      case ValueHolder(v : JValue) => id + " := " + v.extract[String]
      case s@_ => "i forgot something: " + s.toString
    }
  }
}
