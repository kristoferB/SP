package sp.opc

import sp.system.messages.SPIDs
import spray.json._
import sp.domain.ID

object simpleJsonMessToWeb extends SprayJsonSupport {
  // Json conversion
  import DefaultJsonProtocol._

  implicit val uuidFormat = jsonFormat1(JsID)

  case class OpState(id: String, preTrue: JsValue, state: JsValue)
  case class OpOpcDef(id: String, preTrueTag: String, stateTag: String, startTag: String, resetTag: String)
  case class OpWrite(id: String, start: JsValue, reset: JsValue)
  case class VarState(id: String, value: JsValue)
  case class VarOpcDef(id: String, opcTag: String)
  case class VarWrite(id: String, value: JsValue)
  case class OpcDef(operations: List[OpOpcDef], variables: List[VarOpcDef])
  case class JsID(uuid: String)
  case class ConnectionInfo(ip: String, port: Int)
  case class RuntimeState(opcSpecID: String, connected: Boolean, opStates: List[OpState], varStates: List[VarState])

  implicit val opjFormat = jsonFormat3(OpState)
  implicit val opcjFormat = jsonFormat5(OpOpcDef)
  implicit val opwFormat = jsonFormat3(OpWrite)
  implicit val varjFormat = jsonFormat2(VarState)
  implicit val varojFormat = jsonFormat2(VarOpcDef)
  implicit val defFormat = jsonFormat2(OpcDef)
  implicit val varwFormat = jsonFormat2(VarWrite)
  implicit val cejFormat = jsonFormat2(ConnectionInfo)
  implicit val rsjFormat = jsonFormat4(RuntimeState)

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