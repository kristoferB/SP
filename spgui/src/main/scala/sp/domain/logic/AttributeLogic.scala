package sp.domain.logic

import play.api.libs.json._
import scala.util.Try
import org.threeten.bp._
import sp.domain._

/**
  * To use the attributes, you also need to include the json formaters
  * import sp.domain.Logic._ to get it all
  */
object AttributeLogic extends AttributeLogics

trait AttributeLogics {
  // Attribute logic

  implicit def stringToSPValue(x: String): SPValue = SPValue(x)
  implicit def intToSPValue(x: Int): SPValue = SPValue(x)
  implicit def boolToSPValue(x: Boolean): SPValue = SPValue(x)

  implicit class SPValueLogic(value: SPValue) {
    def to[T](implicit fjs: JSReads[T]): Try[T] = {
      Try{ value.as[T] }
    }
    def pretty: String = Json.prettyPrint(value)
    def toJson: String = Json.stringify(value)

    /**
      * Special equal that also handles numbers and bools that are wrapped in strings
      * @param obj
      * @return
      */
    def ===(obj: scala.Any): Boolean = {
      super.equals(obj) ||
        (obj.isInstanceOf[SPValue] &&
          (value.fixStringedTypes == obj.asInstanceOf[SPValue].fixStringedTypes))
    }

    def fixStringedTypes: SPValue = {
      value match {
        case JsString(str) if str.nonEmpty =>
          Try{SPValue(str.toInt)}
            .orElse(Try{SPValue(str.toBoolean)})
            .orElse(Try{SPValue(str.toDouble)})
            .getOrElse(value)
        case _ => value
      }
    }

    def getAs[T](key: String = "")(implicit fjs: JSReads[T]): Option[T] = {
      value match {
        case x: SPAttributes => x.getAs[T](key)
        case x => None
      }
    }
  }




  def timeStamp: SPValue = {
    import JsonLogic._
    Json.toJson(ZonedDateTime.now)
  }


  implicit class SPAttributesLogic(x: SPAttributes) {
    def addTimeStamp(): SPAttributes = {
      x + ("time" -> timeStamp)
    }

    def merge(xs: SPAttributes): SPAttributes = x.deepMerge(xs)

    def get(key: String): Option[SPValue] = {
      x \ key match {
        case JsDefined(res) => Some(res)
        case e: JsUndefined if key.isEmpty => Some(x)
        case e: JsUndefined => None
      }
    }

    def getAs[T](key: String = "")(implicit fjs: JSReads[T]): Option[T] = {
      for {
        x <- get(key)
        t <- x.asOpt[T]
      } yield t
    }
    def to[T](implicit fjs: JSReads[T]): Try[T] = Try{x.as[T]}

    def find(key: String): List[SPValue] = x \\ key toList


    def findAs[T](key: String)(implicit fjs: JSReads[T]): List[T] = {
      find(key).flatMap(_.asOpt[T])
    }

    def findType[T](implicit fjs: JSReads[T]): List[T] = {
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



    def pretty: String = Json.prettyPrint(x)
    def toJson: String  = Json.stringify(x)
  }
}