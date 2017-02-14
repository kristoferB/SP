package sp.messages

import org.json4s.JsonAST
import sp.domain._
import upickle._

import scala.reflect.ClassTag


/**
  * All messages among actors / services should be json-strings with headers and bodies
  * @param header Information about the message as key-value pairs
  * @param body The message encoded as json in uPickle
  */
case class SPMessage(header: SPAttributes, body: upickle.Js.Value) {
  def toJson = APIParser.write(this)
}
object SPMessage {
  def fromJson(x: String) = scala.util.Try{APIParser.read[SPMessage](x)}
}



sealed trait APISP
object APISP {
  case class SPError(message: String, attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPACK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPOK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPDone(attributes: SPAttributes = SPAttributes()) extends APISP

  case class StatusRequest(attributes: SPAttributes = SPAttributes()) extends APISP
  case class StatusResponse(attributes: SPAttributes = SPAttributes()) extends APISP
}




object APIParser extends upickle.AttributeTagged {
  override val tagName = "isa"

  import sp.domain.Logic._

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case x: Js.Obj if n == "org.json4s.JsonAST.JObject" =>
      val res = x.value.map(kv => kv._1 -> fromUpickle(kv._2))
      SPAttributes(res:_*).asInstanceOf[V]
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
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
