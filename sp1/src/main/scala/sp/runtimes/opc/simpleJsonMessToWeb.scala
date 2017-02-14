package sp.runtimes.opc

import sp.domain._


  case class OpState(id: String, preTrue: SPValue, state: SPValue)
  case class OpOpcDef(id: String, preTrueTag: String, stateTag: String, startTag: String, resetTag: String)
  case class OpWrite(id: String, start: SPValue, reset: SPValue)
  case class VarState(id: String, value: SPValue)
  case class VarOpcDef(id: String, opcTag: String)
  case class VarWrite(id: String, value: SPValue)
  case class OpcDef(operations: List[OpOpcDef], variables: List[VarOpcDef])
  case class JsID(uuid: String)
  case class ConnectionInfo(ip: String, port: Int)
  case class RuntimeState(opcSpecID: String, connected: Boolean, opStates: List[OpState], varStates: List[VarState])
