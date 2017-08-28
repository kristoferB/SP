package sp

import java.util.UUID
import scala.util.Try

/**
 * Created by kristofer on 15-05-27.
 */
package object domain {

  /**
    * The default data structure to store info about an item
    * in SP. Is a json structure and can store any case class
    * that has an implicit format defined for it.
    */
  type SPAttributes = play.api.libs.json.JsObject
  type SPValue = play.api.libs.json.JsValue

  /**
    * A helper type that any case class that has a implicit format in
    * scope can be converted into. Else, a compile error will happen.
    */
  type AttributeWrapper = play.api.libs.json.Json.JsValueWrapper
  type JSFormat[T] = play.api.libs.json.Format[T]
  type JSReads[T] = play.api.libs.json.Reads[T]
  type JSWrites[T] = play.api.libs.json.Writes[T]


  /**
    * The id used in SP. A standard UUID.
    */
  type ID = java.util.UUID


  import play.api.libs.json._


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
    def fromJsonGet(json: String, key: String = "") = {
      val res = fromJson(json).toOption
      res.flatMap(get(_, key))
    }
    def fromJsonGetAs[T](json: String, key: String = "")(implicit fjs: JSReads[T]) = {
      val res = fromJson(json)
      getAs[T](res, key)
    }
    def empty = JsObject.empty
    def make[T](x: T)(implicit fjs: JSWrites[T]): SPAttributes = {
      val res = SPValue(x)
      Try{res.asInstanceOf[SPAttributes]}.getOrElse({s"Did not convert to SPAttributes: $x"; empty})
    }

    private def get(x: SPValue, key: String) = {
      x \ key match {
        case JsDefined(res) => Some(res)
        case e: JsUndefined if key.isEmpty => Some(x)
        case e: JsUndefined => None
      }
    }

    private def getAs[T](v: Try[SPValue], key: String = "")(implicit fjs: JSReads[T]) = {
      for {
        vx <- v.toOption
        x <- get(vx, key)
        t <- x.asOpt[T]
      } yield t
    }
  }

  object SPValue {
    def apply[T](v: T)(implicit fjs: JSWrites[T]): SPValue = {
      Json.toJson(v)
    }

    def fromJson(json: String): Try[SPValue] = {
      Try { Json.parse(json) }
    }
    def empty: SPValue = JsObject.empty
  }

  object ID {
    def newID: ID = UUID.randomUUID()
    def makeID(id: String): Option[ID] = Try{UUID.fromString(id)}.toOption
    def isID(str: String): Boolean = makeID(str).nonEmpty
  }


  def fromJsonAs[T](json: String)(implicit fjs: JSReads[T]): Try[T] = {
      SPValue.fromJson(json).flatMap(x => Try{x.as[T]})
  }

  def toJson[T](x: T)(implicit fjs: JSWrites[T]): String = Json.stringify(SPValue(x))









}


