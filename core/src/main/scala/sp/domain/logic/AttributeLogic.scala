package sp.domain.logic

import org.json4s._
//import org.json4s.native.Serialization._

import sp.domain.{SPValue, SPAttributes}

/**
 * You should import AttributeLogic._
 * Also implict val f = formats
 *
 * Created by kristofer on 15-05-26.
 */
object AttributeLogic extends AttributeLogics {

}

trait AttributeLogics {
  import JsonLogic._
  import sp.domain.ID
  //implicit val f = jsonFormats

  implicit def strToJ(x: String): JValue = JString(x)
  implicit def intToJ(x: Int): JValue = JInt(x)
  implicit def boolToJ(x: Boolean): JValue = JBool(x)
  implicit def doubleToJ(x: Double): JValue = JDouble(x)
  implicit def idToJ(x: ID): JValue = JString(x.toString())
//  implicit def pairToSPAttr(p: (String, Any))(implicit formats : org.json4s.Formats): SPAttributes =
//    SPAttributes(p._1->Extraction.decompose(p._2))


  implicit class valueLogic(value: SPValue) {
    def getAs[T](implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
      tryWithOption(
        value.extract[T]
      )
    }
  }

//  implicit class pairAttr(p: (String, Any))(implicit formats : org.json4s.Formats) {
//    val k = p._1
//    val v = Extraction.decompose(p._2)
//    def + : SPAttributes = {
//      val res = List(k->v)
//      SPAttributes(res)
//    }
//    def +(p2: (String, Any)): SPAttributes = {
//      val res = List(k->v, p2._1->Extraction.decompose(p2._2))
//      SPAttributes(res)
//    }
//  }

  implicit class messLogic(x: SPAttributes) {
    val obj = x.obj
    def addTimeStamp = {
      val m = obj.filterNot(_._1 == "time") :+ ("time" -> timeStamp)
      SPAttributes(m)
    }
    def +(kv: (String, Any))(implicit formats : org.json4s.Formats) = {
      SPAttributes(obj :+ kv._1->Extraction.decompose(kv._2))
    }
    def +(xs: JObject) = {
      SPAttributes(obj ++ xs.obj)
    }

    def get(key: String) = {
      x \ key match {
        case JNothing => None
        case res: JValue => Some(res)
      }
      
    }

    def getAs[T](key: String)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
      val res = x \ key
      tryWithOption(
        res.extract[T]
      )
    }

    def find(key: String) = x \\ key match {
      case JObject(xs) => xs.map(_._2)
      case x: JValue => List(x)
    }

    def findAs[T](key: String)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
      for {
        x <- find(key)
        t <- tryWithOption(x.extract[T])
      } yield t
    }

    def findObjectsWithKeys(keys: List[String]) = {
      x.filterField {
        case JField(key, JObject(xs)) => {
          val inObj = xs.map(_._1).toSet
          keys.forall(inObj contains)
        }
        case _ => false
      }
    }
    def findObjectsWithKeysAs[T](keys: List[String])(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
      for {
        value <- findObjectsWithKeys(keys)
        t <- tryWithOption(value._2.extract[T])
      } yield (value._1, t)
    }
    def findObjectsWithField(fields: List[JField]) = {
      x.filterField {
        case JField(key, JObject(xs)) => {
          fields.forall(xs contains)
        }
        case _ => false
      }
    }
    def findObjectsWithFieldAs[T](fields: List[JField])(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
      for {
        value <- findObjectsWithField(fields)
        t <- tryWithOption(value._2.extract[T])
      } yield (value._1, t)
    }

    import org.json4s.native.JsonMethods._
    def pretty = org.json4s.native.JsonMethods.pretty(render(x))
    def toJson = org.json4s.native.JsonMethods.compact(render(x))
  }


  def tryWithOption[T](t: => T): Option[T] = {
    try {
      Some(t)
    } catch {
      case e: Exception => None
    }
  }
}