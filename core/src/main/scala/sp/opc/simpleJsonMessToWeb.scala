package sp.opc

import spray.json._

object simpleJsonMessToWeb extends SprayJsonSupport {
  // Json conversion
  import DefaultJsonProtocol._

  implicit val uuidFormat = jsonFormat1(JsID)

  case class OpState(id: JsID, preTrue: JsValue, state: JsValue)
  case class OpOpcDef(id: JsID, preTrueTag: String, stateTag: String, startTag: String, resetTag: String)
  case class OpWrite(id: JsID, start: Boolean, reset: Boolean)
  case class VarState(id: JsID, value: JsValue)
  case class VarOpcDef(id: JsID, opcTag: String)
  case class VarWrite(id: JsID, value: JsValue)
  case class OpcDef(operations: List[OpOpcDef], variables: List[VarOpcDef])
  case class JsID(uuid: String)

  implicit val opjFormat = jsonFormat3(OpState)
  implicit val opcjFormat = jsonFormat5(OpOpcDef)
  implicit val opwFormat = jsonFormat3(OpWrite)
  implicit val varjFormat = jsonFormat2(VarState)
  implicit val varojFormat = jsonFormat2(VarOpcDef)
  implicit val defFormat = jsonFormat2(OpcDef)
  implicit val varwFormat = jsonFormat2(VarWrite)

}


trait SprayJsonSupport extends spray.httpx.SprayJsonSupport {
  import spray.json._

  implicit object JsObjectWriter extends RootJsonFormat[JsObject] {
    def write(jsObject: JsObject) = jsObject
    def read(value: JsValue) = value.asJsObject
  }

  implicit object JsArrayWriter extends RootJsonFormat[JsArray] {
    def write(jsArray: JsArray) = jsArray
    def read(value: JsValue) = value.asInstanceOf[JsArray]
  }
}