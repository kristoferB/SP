package sp

import org.json4s.JsonAST.JValue
import org.json4s._

/**
 * Created by kristofer on 15-05-27.
 */
package object domain {

  type SPAttributes = JObject
  //val SPAttributes = JObject
  type SPValue = JValue
  //val SPValue = JValue

  object SPAttributes {
    def apply[T](pair: (String, T)*)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]): SPAttributes = {
      val res = pair.map{
        case (key, value) => key -> SPValue(value)
      }
      JObject(res.toList)
    }
    def apply(): SPAttributes = JObject()
    def apply(fs: List[JField]): SPAttributes = JObject(fs.toList)
    def fromJson(json: String) = {
      try {
        org.json4s.native.JsonMethods.parse(json) match {
          case x: SPAttributes => Some(x)
          case x: JValue => None
        }
      } catch {
        case e: Exception => None
      }
    }

    import upickle.Js
    import sp.domain.Logic._


  }

  object SPValue {
    def apply[T](v: T)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]): SPValue = {
      Extraction.decompose(v)
    }
    def apply(s: String): SPValue = JString(s)
    def apply(i: Int): SPValue = JInt(i)
    def apply(b: Boolean): SPValue = JBool(b)
    def fromJson(json: String): Option[SPValue] = {
      try {
        Some(org.json4s.native.JsonMethods.parse(json))
      } catch {
        case e: Exception => None
      }
    }


  }


}


