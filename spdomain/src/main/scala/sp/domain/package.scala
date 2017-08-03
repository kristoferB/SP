package sp

import java.util.UUID
import play.api.libs.json._
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
  type SPAttributes = JsObject
  type SPValue = JsValue

  /**
    * A helper type that any case class that has a implicit format in
    * scope can be converted into. Else, a compile error will happen.
    */
  type AttributeWrapper = Json.JsValueWrapper
  type JSFormat[T] = OFormat[T]
  type JSReads[T] = Reads[T]
  type JSWrites[T] = OWrites[T]

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
    def fromJsonAs[T](json: String, key: String = "")(implicit fjs: JSReads[T]) = {
      val res = fromJson(json)

    }
    def empty = JsObject.empty
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
    def newID: ID = UUID.randomUUID()
    def makeID(id: String): Option[ID] = Try{UUID.fromString(id)}.toOption
    def isID(str: String): Boolean = makeID(str).nonEmpty
  }











}


