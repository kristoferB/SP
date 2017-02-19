package sp

import upickle._
import scala.util.Try

/**
  * Created by kristofer on 2017-02-15.
  */
package object domain {
  import sp.messages._
  import sp.messages.Pickles._


  type SPAttributes = upickle.Js.Obj
  type SPValue = upickle.Js.Value

  object SPValue {
    def apply[T: Writer](expr: T): SPValue = Pickles.toSPValue(expr)
  }

  object SPAttributes {
    /**
      * This metod will throw an exception if the expr is not a case class or a map
      * @param expr The object to convert to SPAttributes
      * @tparam T The type of the object. Is usually infereed
      * @return An SPAttributes or throws an exception
      */
    def apply[T: Writer](expr: T): SPAttributes = **(expr)
    def apply(map: Map[String, SPValue] = Map()): SPAttributes = upickle.Js.Obj(map.toSeq:_*)
    def fromJson(x: String): Option[SPAttributes] = fromJsonToSPAttributes(x).toOption
  }



  implicit class valueLogic(value: SPValue) {
    def getAs[T: Reader](key: String = "") = {
      val x: SPValue = Try{value.obj(key)}.getOrElse(value)
      Try{readJs[T](x)}.toOption
    }

    def /(key: String) = Try{value.obj(key)}.toOption

    def toJson = upickle.json.write(value)

  }

  implicit class valueLogicOption(value: Option[SPValue]) {
    def getAs[T: Reader](key: String = "") = {
      val x = Try{value.get.obj.get(key)}.getOrElse(value)
      x.flatMap(v => Try{readJs[T](v)}.toOption)
    }

    def /(key: String) = Try{value.get.obj(key)}.toOption

  }


}





