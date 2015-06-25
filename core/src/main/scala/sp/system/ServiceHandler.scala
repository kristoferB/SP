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
case class KeyDefinition(isa: String, default: Option[SPValue], definitions: Option[SPAttributes])

class ServiceTalker(service: ActorRef, modelHandler: ActorRef, replyTo: ActorRef, serviceAttributes: SPAttributes,  toBus: Option[ActorRef]) extends Actor {
  import sp.domain.Logic._
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.util._
  import scala.concurrent.duration._
  import context.dispatcher
  implicit val timeout = Timeout(2 seconds)

  val model = serviceAttributes.getAs[ID]("model")
  val toModel = serviceAttributes.getAs[Boolean]("toModel").getOrElse(false) && model.isDefined
  val expectAttrs = extractKeyAttributes(serviceAttributes)

  def receive = {
    case r @ Request(_, attr, ids) => {
      val errors = analyseAttr(attr, expectAttrs)
      if (errors.nonEmpty) replyTo ! SPErrors(errors)
      else {
        if (model.isDefined && ids.isEmpty) {
          modelHandler ? GetIds(model.get, List()) onComplete {
            case Success(result) => result match {
              case SPIDs(xs) => service ! r.copy(ids = xs)
            }
            case Failure(failure) => replyTo ! SPError(failure.getMessage)
          }
        } else {
          service ! r
        }
      }
    }
    case r @ Response(ids, attr) => {
      if (toModel) {
        modelHandler ! UpdateIDs(model.get, -1, ids, attr)
      }
      replyTo ! r
      toBus.foreach(_ ! r)
      self ! PoisonPill
    }
    case r @ Progress(attr) => {
      replyTo ! r
      toBus.foreach(_ ! r)
      self ! PoisonPill
    }


  }

  def analyseAttr(attr: SPAttributes, expected: List[(String, KeyDefinition)]): List[SPError] = {
    expected.flatMap{ case (key, v) =>
      attr.get(key).getOrElse(v.default.getOrElse(JNothing)) match {
        case o: JObject => {
          v.definitions.map(d => analyseAttr(o, extractKeyAttributes(d))).getOrElse(List())
        }
        case JNothing => List(SPError(s"requiered key: $key is missing"))
        case _ => List()
      }
    }
  }

  def extractKeyAttributes(x: SPAttributes) = x.findObjectsWithKeysAs[KeyDefinition](List("isa"))

}

object ServiceTalker {
  def props(service: ActorRef,
            modelHandler: ActorRef,
            replyTo: ActorRef,
            serviceAttributes: SPAttributes,
            toBus: Option[ActorRef] = None) =
    Props(classOf[ServiceTalker], service, modelHandler, replyTo, serviceAttributes, toBus)
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