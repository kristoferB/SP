package sp.system

import akka.actor._
import org.json4s.{JNothing}
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._
import akka.pattern.ask
import akka.util.Timeout
import scala.util._
import scala.concurrent.duration._

class ServiceTalker(service: ActorRef,
                    modelHandler: ActorRef,
                    replyTo: ActorRef,
                    serviceAttributes: SPAttributes,
                    request: Request,
                    eventHandler: Option[ActorRef]) extends Actor {

  import context.dispatcher
  implicit val timeout = Timeout(2 seconds)
  val cancelTimeout =  context.system.scheduler.scheduleOnce(3 seconds, self, "timeout")

  val reqAttr = request.attributes
  val model = reqAttr.dig[ID]("service", "model")
  val toModel = reqAttr.getAs[Boolean]("toModel").getOrElse(false) && model.isDefined
  val onlyResponse = reqAttr.getAs[Boolean]("onlyResponse").getOrElse(false)
  val fillIDs = reqAttr.getAs[List[ID]]("fillIDs").getOrElse(List()).toSet

  def receive = {
    case req @ Request(_, attr, ids, _) => {
      if (model.isDefined && ids.isEmpty) {
        modelHandler ? GetIds(model.get, List()) onComplete {
          case Success(result) => result match {
            case SPIDs(xs) => {
              val filter = xs.filter(item => fillIDs.contains(item.id))
              val res = if (filter.nonEmpty) filter else xs
              service ! req.copy(ids = res)
            }
            case x => {
              replyTo ! x
              killMe
            }
          }
          case Failure(failure) => {
            replyTo ! SPError(failure.getMessage)
            killMe
          }
        }
      } else {
        service ! req
      }
    }
    case "timeout" => {
      replyTo ! SPError(s"Service ${request.service} (actor: $service) is not responding.")
      eventHandler.foreach(_ ! SPError(s"Service ${request.service} (actor: $service) is not responding."))
//      context.parent ! RemoveService(request.service)
//      service ! RemoveService(request.service)
      killMe
    }
    case r @ Response(ids, attr, _, _) => {
      if (toModel) {
        modelHandler ! UpdateIDs(model.get, ids, attr)
      }
      replyTo ! r
      eventHandler.foreach(_ ! r)
      killMe
    }
    case r: Progress => {
      cancelTimeout.cancel()
      if (!onlyResponse) replyTo ! r
      eventHandler.foreach(_ ! r)
    }
    case e: SPError => {
      replyTo ! e
      eventHandler.foreach(_ ! ServiceError(request.service, request.reqID, e))
      killMe
    }
    case x if sender() == service => {
      println("SERVICES SHOULD SEND case class Response, NOT "+x)
      replyTo ! x
      killMe
    }
  }

  def killMe = {
    cancelTimeout.cancel()
    self ! PoisonPill
  }

}

object ServiceTalker {
  def props(service: ActorRef,
            modelHandler: ActorRef,
            replyTo: ActorRef,
            serviceAttributes: SPAttributes,
            request: Request,
            toBus: Option[ActorRef]) =
    Props(classOf[ServiceTalker], service, modelHandler, replyTo, serviceAttributes, request, toBus)

  def validateRequest(req: Request, serviceAttributes: SPAttributes) = {
    val attr = req.attributes
    val expectAttrs = serviceAttributes.findObjectsWithKeysAs[KeyDefinition](List("ofType", "domain"))

    val errors = analyseAttr(attr, expectAttrs)
    if (errors.nonEmpty) Left(errors) else {
      val filled = req.copy(attributes = fillDefaults(attr, expectAttrs))
      Right(filled)
    }
  }

  def serviceSpec = SPAttributes{
    "model"-> KeyDefinition("ID", List(), Some(false)),
  }

  private def analyseAttr(attr: SPAttributes, expected: List[(String, KeyDefinition)]): List[SPError] = {
    expected.flatMap{ case (key, v) =>
      val flatAttr = attr.filterField{x => true}.toMap
      flatAttr.get(key).getOrElse(v.default.getOrElse(JNothing)) match {
        case JNothing => List(SPError(s"required key $key is missing"))
        case _ => List()
      }
    }
  }

  private def fillDefaults(attr: SPAttributes, expected: List[(String, KeyDefinition)]): SPAttributes = {
    val flatAttr = attr.filterField{x => true}.toMap
    val d = expected.filter(kv => !flatAttr.contains(kv._1)).map(kv => kv._1 -> kv._2.default.getOrElse(JNothing))
    attr + SPAttributes(d)
  }

}

