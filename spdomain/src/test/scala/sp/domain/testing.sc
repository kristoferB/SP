import java.util.UUID

import play.api.libs.json._
import scala.util.Try
import org.threeten.bp._

trait domain {
  /**
    * The default data structure to store info about an item
    * in SP. Is a json structure and can store any case class
    * that has an implicit format defined for it.
    */
  type SPAttributes = JsObject
  type SPValue = JsValue

  /**
    * A helper type that any case class that has a implicit format in
    * scope can be converted into. Else, a compile error will happen.
    */
  type AttributeWrapper = Json.JsValueWrapper

  /**
    * The id used in SP. A standard UUID.
    */
  type ID = java.util.UUID

  object SPAttributes {
    def apply(pair: (String, AttributeWrapper)*): SPAttributes = {
      Json.obj(pair:_*)
    }
    def apply(): SPAttributes = JsObject.empty
    def fromJson(json: String): Try[SPAttributes] = {
      Try {
        Json.parse(json).asInstanceOf[SPAttributes]
      }
    }
  }

  object SPValue {
    def apply(v: AttributeWrapper): SPValue = {
      // should always work
      // hack to be able to use JsonJsValueWrapper
      Json.arr(v).value.head
    }

    def fromJson(json: String): Try[SPValue] = {
      Try { Json.parse(json) }
    }
    def empty: SPValue = JsObject.empty
  }

  object ID {
    def newID = UUID.randomUUID()
    def makeID(id: String): Option[ID] = Try{UUID.fromString(id)}.toOption
    def isID(str: String) = makeID(str).nonEmpty
  }


}

implicit class SPValueLogic(value: Test.SPValue) {
  def to[T](implicit fjs: Reads[T]) = {
    Try{ value.as[T] }.toOption
  }
  def pretty = Json.prettyPrint(value)
  def toJson = Json.stringify(value)
}


val dateF = format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
implicit val javatimeF = new Format[ZonedDateTime] {
  override def reads(json: JsValue): JsResult[ZonedDateTime] = {
    json.validate[String].map(ZonedDateTime.parse(_, dateF))
  }

  override def writes(o: ZonedDateTime): JsValue = {
    Json.toJson(o.format(dateF))
  }

}

def timeStamp = {
  Json.toJson(ZonedDateTime.now)
}


implicit class SPAttributesLogic(x: Test.SPAttributes) {

  def addTimeStamp = {
    x + ("time" -> timeStamp)
  }

  def merge(xs: Test.SPAttributes) = x.deepMerge(xs)

  def get(key: String) = {
    x \ key match {
      case JsDefined(res) => Some(res)
      case e: JsUndefined if key.isEmpty => Some(x)
      case e: JsUndefined => None
    }
  }

  def getAs[T](key: String)(implicit fjs: Reads[T]) = {
    for {
      x <- get(key)
      t <- x.asOpt[T]
    } yield t
  }

  def find(key: String) = x \\ key toList


  def findAs[T](key: String)(implicit fjs: Reads[T]) = {
    find(key).flatMap(_.asOpt[T])
  }

  def findType[T](implicit fjs: Reads[T]): List[T] = {
    def extrType(xs: List[JsValue]): List[T] = {
      xs.collect {
        case l: JsObject =>
          l.asOpt[T] match {
            case Some(found) => List(found)
            case None => l.findType[T]
          }
        case l: JsArray =>
          extrType(l.value.toList)
      }.flatten
    }
      extrType(x.values.toList)
  }



  def pretty = Json.prettyPrint(x)
  def toJson = Json.stringify(x)
}




object Test extends domain {

}

import julienrf.json.derived


object API {
  sealed trait Kalle
  case class Testing(a1: String, b2: Int = 5) extends Kalle
  case class Testing2(a3: String, b4: Int = 5) extends Kalle
  case class Testing3(k: Kalle) extends Kalle

  object Kalle {
    //implicit val f: Reads[Kalle] = derived.flat.reads((__ \ "isa").read[String])
    //implicit val f2: OWrites[Kalle] = derived.flat.owrites((__ \ "isa").write[String])
    implicit val f3: OFormat[Kalle] = derived.flat.oformat[Kalle]((__ \ "isa").format[String])
  }
}


//implicit val residentFormat = Json.format[Testing]

val id = Test.ID.newID

val k = API.Testing3(API.Testing("hej"))

val k2 = Test.SPAttributes("hej" -> ("nu" -> k), "då" -> k).addTimeStamp
val k3 = Json.stringify(k2)



val k4 = Test.SPAttributes.fromJson(k3)

val res = Test.SPValue(k)
val back = Json.fromJson[API.Kalle](res)

k2.getAs[API.Kalle]("då")

k2.findType[API.Kalle]

val one = Test.SPValue(1)
val one2 = Test.SPValue(1.1)

one == one2