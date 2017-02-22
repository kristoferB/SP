package sp.messages

import sp.domain._
import java.util.UUID



sealed trait APISP
object APISP {
  case class SPError(message: String, attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPACK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPOK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPDone(attributes: SPAttributes = SPAttributes()) extends APISP

  case class StatusRequest(attributes: SPAttributes = SPAttributes()) extends APISP
  case class StatusResponse(attributes: SPAttributes = SPAttributes()) extends APISP
}



object Pickles extends SPParser {

  import scala.util.{Try, Success, Failure}
  import sp.domain._

  type Pickle = upickle.Js.Value


  case class SPHeader(from: String,
                       to: String = "",
                       replyTo: String = "",
                       reqID: UUID = UUID.randomUUID(),
                       replyFrom: String = "",
                       replyID: Option[UUID] = None)


  case class SPMessage(header: Pickle, body: Pickle) {
    def getHeaderAs[T: Reader] = fromPickle[T](header)
    def getBodyAs[T: Reader] = fromPickle[T](body)

    def toJson = write(this)

    def extendHeader[T: Writer](w: T) = {
      val p = toPickle[T](w)
      val updH = header.union(p)
      SPMessage(updH, body)
    }

    /**
      * Creates an updated SPMessage that keeps the header keyvals that is not defined in h
      * @param h The upd key vals in the header
      * @param b The new body
      * @return an updated SPMessage
      */
    def make[T: Writer, V: Writer](h: T, b: V) = {
      Try {
        val newh = toPickle[T](h)
        val newb = toPickle[V](b)
        val updH = header.union(newh)
        SPMessage(updH, newb)
      }
    }
    def makeJson[T: Writer, V: Writer](header: T, body: V) = {
      this.make[T, V](header, body).map(_.toJson)
    }
  }

  object SPMessage {
    def make[T: Writer, V: Writer](header: T, body: V) = {
      Try{
        val h = toPickle[T](header)
        val b = toPickle[V](body)
        SPMessage(h, b)
      }
    }
    def makeJson[T: Writer, V: Writer](header: T, body: V) = {
      make[T, V](header, body).map(_.toJson)
    }

    def fromJson(json: String) = Try{
      val x = upickle.json.read(json)
      SPMessage(x.obj("header"), x.obj("body"))
    }
  }



  implicit class pickleLogic(value: Pickle) {
    def getAs[T: Reader](key: String = "") = {
      getAsTry[T](key).toOption
    }

    def getAsTry[T: Reader](key: String = "") = {
      val x: Pickle = Try{value.obj(key)}.getOrElse(value)
      Try{readJs[T](x)}
    }

    def /(key: String) = Try{value.obj(key)}.toOption

    def toJson = upickle.json.write(value)

    def union(p: Pickle) = {
      Try{
        val map = value.obj ++ p.obj
        upickle.Js.Obj(map.toSeq:_*)
      }.getOrElse(value)
    }

  }


  implicit class pickleLogicOption(value: Option[Pickle]) {
    def getAs[T: Reader](key: String = "") = {
      val x = Try{value.get.obj.get(key)}.getOrElse(value)
      x.flatMap(v => Try{readJs[T](v)}.toOption)
    }

    def /(key: String) = Try{value.get.obj(key)}.toOption
  }

  def toJson[T: Writer](expr: T, indent: Int = 0): String = upickle.json.write(writeJs(expr), indent)
  def toPickle[T: Writer](expr: T): Pickle = implicitly[Writer[T]].write(expr)
  def *[T: Writer](expr: T): Pickle = toPickle[T](expr)

  def fromJson[T: Reader](expr: String): Try[T] = Try{readJs[T](upickle.json.read(expr))}
  def fromPickle[T: Reader](expr: Pickle): Try[T] = Try{implicitly[Reader[T]].read(expr)}
  def fromJsonToPickle[T: Reader](expr: String): Try[Pickle] = Try{upickle.json.read(expr)}


}


trait SPParser extends upickle.AttributeTagged {
  override val tagName = "isa"

  import upickle._
  import org.json4s.JsonAST
  import sp.domain._
  import sp.domain.Logic._
  import scala.reflect.ClassTag

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case x: Js.Obj if n == "org.json4s.JsonAST.JObject" =>
      val res = x.value.map(kv => kv._1 -> fromUpickle(kv._2))
      SPAttributes(res:_*).asInstanceOf[V]
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
    case x if n.contains("org.json4s.JsonAST") => fromUpickle(x).asInstanceOf[V]
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{
    case x: SPValue =>
      toUpickle(x)
    case x: V =>
      val filter = n.split('.').takeRight(2).mkString(".")
      Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }

  def toUpickle(value: SPValue): upickle.Js.Value = value match {
    case x: JsonAST.JBool => upickle.default.writeJs(x.values)
    case x: JsonAST.JDecimal => upickle.default.writeJs(x.values)
    case x: JsonAST.JDouble => upickle.default.writeJs(x.values)
    case x: JsonAST.JInt => upickle.default.writeJs(x.values)
    case x: JsonAST.JLong => upickle.default.writeJs(x.values)
    case x: JsonAST.JString => upickle.default.writeJs(x.values)
    case x: JsonAST.JObject =>
      val res = x.obj.map(kv => kv._1 -> toUpickle(kv._2))
      upickle.Js.Obj(res:_*)
    case x: JsonAST.JArray => upickle.Js.Arr(x.arr.map(toUpickle):_*)
    case x => upickle.Js.Null
  }
  def fromUpickle(value: upickle.Js.Value): SPValue = value match {
    case x: upickle.Js.Str => SPValue(x.value)
    case x: upickle.Js.Arr => SPValue(x.value.map(fromUpickle))
    case x: upickle.Js.Num => SPValue(x.value)
    case upickle.Js.False => SPValue(false)
    case upickle.Js.True => SPValue(true)
    case upickle.Js.Null => SPValue(None)
    case x: upickle.Js.Obj =>
      val json = upickle.json.write(value)
      SPValue.fromJson(json).getOrElse(SPValue("ERROR_UPICKLE"))

  }


}




//object APIParser extends upickle.AttributeTagged {
//  override val tagName = "isa"
//
//  import sp.domain.Logic._
//
//  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
//    case x: Js.Obj if n == "org.json4s.JsonAST.JObject" =>
//      val res = x.value.map(kv => kv._1 -> fromUpickle(kv._2))
//      SPAttributes(res:_*).asInstanceOf[V]
//    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
//      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
//  }
//
//  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{
//    case x: SPValue =>
//      toUpickle(x)
//    case x: V =>
//    val filter = n.split('.').takeRight(2).mkString(".")
//    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
//  }
//
//  def toUpickle(value: SPValue): upickle.Js.Value = value match {
//    case x: JsonAST.JBool => upickle.default.writeJs(x.values)
//    case x: JsonAST.JDecimal => upickle.default.writeJs(x.values)
//    case x: JsonAST.JDouble => upickle.default.writeJs(x.values)
//    case x: JsonAST.JInt => upickle.default.writeJs(x.values)
//    case x: JsonAST.JLong => upickle.default.writeJs(x.values)
//    case x: JsonAST.JString => upickle.default.writeJs(x.values)
//    case x: JsonAST.JObject =>
//      val res = x.obj.map(kv => kv._1 -> toUpickle(kv._2))
//      upickle.Js.Obj(res:_*)
//    case x: JsonAST.JArray => upickle.Js.Arr(x.arr.map(toUpickle):_*)
//    case x => upickle.Js.Null
//  }
//  def fromUpickle(value: upickle.Js.Value): SPValue = value match {
//    case x: upickle.Js.Str => SPValue(x.value)
//    case x: upickle.Js.Arr => SPValue(x.value.map(fromUpickle))
//    case x: upickle.Js.Num => SPValue(x.value)
//    case upickle.Js.False => SPValue(false)
//    case upickle.Js.True => SPValue(true)
//    case upickle.Js.Null => SPValue(None)
//    case x: upickle.Js.Obj =>
//      val json = upickle.json.write(value)
//      SPValue.fromJson(json).getOrElse(SPValue("ERROR_UPICKLE"))
//
//  }
//
//
//
//}