package sp.system

import akka.actor._
import akka.event.Logging
import sp.domain._
import sp.system.messages._

class ServiceHandler extends Actor{
  var actors: Map[String, ActorRef] = Map()
  val log = Logging(context.system, this)
  
  def receive = {
    case r @ RegisterService(service, ref) => {
      if (!actors.contains(service)) {
        actors += service -> ref
        ref.tell(r, sender)
      }
      else sender ! SPError(s"Service $service already registered")
    }
    case m: ServiceMessage => {
      if (actors.contains(m.service))
        actors(m.service).tell(m, sender)
      else sender ! SPError(s"Service ${m.service} does not exists")
    }
    case GetServices => sender ! actors.keys.toList
  }
}

object ServiceHandler {
  def props = Props(classOf[ServiceHandler])
}



case class ServiceRequest(model: ID, attr: SPAttributes, fromModel: Map[String, SPIDs])

abstract class ServiceHelper(modelHandler: ActorRef) extends Actor {
  /**
   * Defines the input to the service. Use DefinitionPrimitive if
   * attributes is required. Use a default value if the attribute
   * can be skipped.
   */
  val interface: Map[String, SPAttributeValue]





  def request(request: ServiceRequest): Unit


  def receive = {
    case "hej" => sender ! "då"
    case Request(_, attr) => {
      val reply = sender
      val model = getModel(attr)
      val filled = fillInterface(attr, interface.toList)
      println(s"filled $interface with $attr, and got: $filled")

      if (model.isLeft) reply ! model.left.get
      else if (filled.isLeft) reply ! filled.left.get
      else {
        val modelID = model.right.get
        val res = filled.right.get
        request(ServiceRequest(modelID, SPAttributes(res), Map()))
      }

    }
  }

  def fetchFromModel(attrs: List[String]): Map[String, SPIDs] ={
    return attrs.map(_ -> SPIDs(List())).toMap
  }

  private def getModel(attr: SPAttributes): Either[SPError, ID] = {
    attr.getAsID("model") match {
      case Some(id) => Right(id)
      case None => Left(SPError(s"A model id can not be found in: $attr"))
    }
  }

  private def fillInterface(attr: SPAttributes, interface: List[(String, SPAttributeValue)]): Either[List[SPError], Map[String, SPAttributeValue] ] = {
    interface match {
      case Nil => Right(Map())
      case (a, v) :: xs => {
        val res: Either[List[SPError], Map[String, SPAttributeValue] ] = parse(attr, a, v)
        val otherRes = fillInterface(attr, xs)

        val errors: List[SPError] = otherRes.left.getOrElse(List()) ++ res.left.getOrElse(List())
        if (errors.isEmpty){
          for {
            m1 <- res.right
            m2 <- otherRes.right
          } yield m1 ++ m2
        } else
          Left(errors)
      }
    }
  }

  private def parse(parse: SPAttributes, attribute: String, valueType: SPAttributeValue):
                    Either[List[SPError], Map[String, SPAttributeValue] ] = {


    (parse.get(attribute), valueType) match {
      case (None, DefinitionPrimitive(t, None)) =>
        Left(List(SPError(s"Required Attribute $attribute, of type ${valueType.asInstanceOf[DefinitionPrimitive].definition}, is missing")))
      case (None, DefinitionPrimitive(t, Some(default))) =>
        Right(Map(attribute->default))
      case (Some(got), d: DefinitionPrimitive) => {
        got.asDef(d) match {
          case Some(value) => Right(Map(attribute->value))
          case None => Left(List(SPError(s"Attribute $attribute is of type ${got.getClass}, should be ${d.definition}")))
        }
      }
      case (Some(got: MapPrimitive), d: MapPrimitive) => {
        fillInterface(got.asSPAttributes, d.value.toList)
      }
      case (Some(got: ListPrimitive), d: ListPrimitive) => {
        if (d.value.isEmpty || !d.value.head.isInstanceOf[DefinitionPrimitive])
          Left(List(SPError(s"First element in list attribute  $attribute, must be an definition primitive. Now: $d")))
        else {
          val t = got.value.flatMap(_.asDef(d.value.head.asInstanceOf[DefinitionPrimitive]))
          Right(Map(attribute->ListPrimitive(t)))
        }

      }
    }

  }





}