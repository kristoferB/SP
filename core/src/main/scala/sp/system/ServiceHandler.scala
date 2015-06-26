package sp.system

import akka.actor._
import akka.event.Logging
import org.json4s.JsonAST.{JObject, JNothing}
import sp.domain._
import sp.system.messages._

class ServiceHandler extends Actor{
  val log = Logging(context.system, this)
  var actors: Map[String, ActorRef] = Map()
  var info: Map[String, SPAttributes] = Map()

  def receive = {
    case r @ RegisterService(service, ref, attr) => {
      if (!actors.contains(service)) {
        actors += service -> ref
        info = info + (service -> attr)
        ref.tell(r, sender)
      }
      else sender ! SPError(s"Service $service already registered")
    }
    case m: ServiceMessage => {
      if (actors.contains(m.service))
        actors(m.service).tell(m, sender)
      else sender ! SPError(s"Service ${m.service} does not exists")
    }
    case GetServices => sender ! Services(info)
  }
}

object ServiceHandler {
  def props = Props(classOf[ServiceHandler])
}

// move somewhere good
case class KeyDefinition(ofType: String, domain: List[SPValue] = List(), default: Option[SPValue] = None)


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
                    busHandler: Option[ActorRef]) extends Actor {

  import context.dispatcher
  implicit val timeout = Timeout(2 seconds)

  val expectAttrs = serviceAttributes.findObjectsWithKeysAs[KeyDefinition](List("ofType", "domain"))
  val reqAttr = request.attributes
  val model = reqAttr.getAs[ID]("model")
  val toModel = reqAttr.getAs[Boolean]("toModel").getOrElse(false) && model.isDefined
  val toBus = reqAttr.getAs[Boolean]("toBus").getOrElse(false) && busHandler.isDefined
  val onlyResponse = reqAttr.getAs[Boolean]("onlyResponse").getOrElse(false)
  val fillIDs = reqAttr.getAs[List[ID]]("fillIDs").getOrElse(List()).toSet

  def receive = {
    case req @ Request(_, attr, ids) => {
        if (model.isDefined && ids.isEmpty) {
          modelHandler ? GetIds(model.get, List()) onComplete {
            case Success(result) => result match {
              case SPIDs(xs) => {
                val filter = xs.filter(item => fillIDs.contains(item.id))
                val res = if (filter.nonEmpty) filter else xs
                service ! req.copy(ids = res)
              }
            }
            case Failure(failure) => replyTo ! SPError(failure.getMessage)
          }
        } else {
          service ! req
        }
    }

    case r @ Response(ids, attr) => {
      if (toModel) {
        modelHandler ! UpdateIDs(model.get, ids, attr)
      }
      replyTo ! r
      if (toBus) busHandler.foreach(_ ! r)
      self ! PoisonPill
    }
    case r: Progress => {
      if (!onlyResponse) replyTo ! r
      if (toBus) busHandler.foreach(_ ! r)
    }
  }

  def fillDefaults(attr: SPAttributes, expected: List[(String, KeyDefinition)]): SPAttributes = {
    val flatAttr = attr.filterField{x => true}.toMap
    val d = expected.filter(kv => !flatAttr.contains(kv._1)).map(kv => kv._1 -> kv._2.default.getOrElse(JNothing))
    attr + SPAttributes(d)
  }


}

object ServiceTalker {
  def props(service: ActorRef,
            modelHandler: ActorRef,
            replyTo: ActorRef,
            serviceAttributes: SPAttributes,
            request: Request,
            toBus: Option[ActorRef] = None) =
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


//case class ServiceRequest(modelID: ID, attr: SPAttributes, model: SPIDs)
//
//abstract class ServiceHelper(modelHandler: ActorRef) extends Actor {
//  /**
//   * Defines the input to the service. Use DefinitionPrimitive if
//   * attributes is required. Use a default value if the attribute
//   * can be skipped.
//   */
//  val interface: Map[String, SPValueDefinition]
//
//
//
//
//
//  def request(request: ServiceRequest): Unit
//
//
//  def receive = {
//    case "hej" => sender ! "då"
//    case Request(_, attr) => {
//      val reply = sender
//      val model = getModel(attr)
//      val filled = fillInterface(attr, interface.toList)
//      println(s"filled $interface with $attr, and got: $filled")
//
//      if (model.isLeft) reply ! model.left.get
//      else if (filled.isLeft) reply ! filled.left.get
//      else {
//        val modelID = model.right.get
//        val res = filled.right.get
//        request(ServiceRequest(modelID, SPAttributes(res), Map()))
//      }
//
//    }
//  }
//
//  def fetchFromModel(attrs: List[String]): Map[String, SPIDs] ={
//    return attrs.map(_ -> SPIDs(List())).toMap
//  }
//
//  private def getModel(attr: SPAttributes): Either[SPError, ID] = {
//    attr.getAsID("model") match {
//      case Some(id) => Right(id)
//      case None => Left(SPError(s"A model id can not be found in: $attr"))
//    }
//  }
//
//  private def fillInterface(attr: SPAttributes, interface: List[(String, SPValue)]): Either[List[SPError], Map[String, SPValue] ] = {
//    interface match {
//      case Nil => Right(Map())
//      case (a, v) :: xs => {
//        val res: Either[List[SPError], Map[String, SPValue] ] = parse(attr, a, v)
//        val otherRes = fillInterface(attr, xs)
//
//        val errors: List[SPError] = otherRes.left.getOrElse(List()) ++ res.left.getOrElse(List())
//        if (errors.isEmpty){
//          for {
//            m1 <- res.right
//            m2 <- otherRes.right
//          } yield m1 ++ m2
//        } else
//          Left(errors)
//      }
//    }
//  }
//
//  private def parse(parse: SPAttributes, attribute: String, valueType: SPValue):
//                    Either[List[SPError], Map[String, SPValue] ] = {
//
//
//    (parse.get(attribute), valueType) match {
//      case (None, DefinitionPrimitive(t, None)) =>
//        Left(List(SPError(s"Required Attribute $attribute, of type ${valueType.asInstanceOf[DefinitionPrimitive].definition}, is missing")))
//      case (None, DefinitionPrimitive(t, Some(default))) =>
//        Right(Map(attribute->default))
//      case (Some(got), d: DefinitionPrimitive) => {
//        got.asDef(d) match {
//          case Some(value) => Right(Map(attribute->value))
//          case None => Left(List(SPError(s"Attribute $attribute is of type ${got.getClass}, should be ${d.definition}")))
//        }
//      }
//      case (Some(got: MapPrimitive), d: MapPrimitive) => {
//        fillInterface(got.asSPAttributes, d.value.toList)
//      }
//      case (Some(got: ListPrimitive), d: ListPrimitive) => {
//        if (d.value.isEmpty || !d.value.head.isInstanceOf[DefinitionPrimitive])
//          Left(List(SPError(s"First element in list attribute  $attribute, must be an definition primitive. Now: $d")))
//        else {
//          val t = got.value.flatMap(_.asDef(d.value.head.asInstanceOf[DefinitionPrimitive]))
//          Right(Map(attribute->ListPrimitive(t)))
//        }
//
//      }
//    }
//
//  }





//}