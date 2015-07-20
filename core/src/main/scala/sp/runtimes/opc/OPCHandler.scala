package sp.runtimes.opc

import akka.actor._
import sp.domain.Logic._
import sp.domain._

case object GetOpStates
case object GetVarStates
case class UpdateOpcSubscription(opcDef: OpcDef)

trait StateVariable {
  val name: String
  val attributes: SPAttributes
}

class OPCHandler(listener: ActorRef) extends Actor with akka.actor.ActorLogging {

  def receive = {
    case OPCValue(opcTag, value: SPValue) => updateState(opcTag, value)
    case GetOpStates => sender ! opStates
    case GetVarStates => sender ! varStates
    case OpWrite(id, start, reset) => writeOp(id, start, reset)
    case VarWrite(id, value) => writeVar(id, value)
    case UpdateOpcSubscription(opcDef: OpcDef) => updateOpcSubscription(opcDef)
    case l: List[_] => println(l) // forward error messages
  }

  var opStates: Map[String, OpState] = Map.empty
  var varStates: Map[String, VarState] = Map.empty
  private var opOpcDefs: Map[String, OpOpcDef] = Map.empty
  private var varOpcDefs: Map[String, VarOpcDef] = Map.empty
  
  private def updateState(opcTag: String, value: SPValue) = {
    if (opOpcDefs.contains(opcTag)){
      val opOpcDef: OpOpcDef = opOpcDefs(opcTag)
      val oldOpState: OpState = opStates(opOpcDef.id)
      val newOpState: OpState = if (opOpcDef.preTrueTag == opcTag) {
        oldOpState.copy(preTrue=value)
      } else if (opOpcDef.stateTag == opcTag) {
        oldOpState.copy(state = value)
      } else
        oldOpState
      opStates += opOpcDef.id -> newOpState
      listener ! newOpState
    }
    if (varOpcDefs.contains(opcTag)){
      val varOpcDef: VarOpcDef = varOpcDefs(opcTag)
      val oldVarState: VarState = varStates(varOpcDef.id)
      val newVarState: VarState = oldVarState.copy(value = value)
      varStates += varOpcDef.id -> newVarState
      listener ! newVarState
    }
  }
  
  private def writeOp(id: String, start: SPValue, reset: SPValue) = {
    println("Server should write: " + id + " start:" + start + " reset: " + reset)
    for {
      op <- opOpcDefs.values find (_.id == id)
    } yield {
      println(op)
      listener ! OPCWrite(Map(op.startTag -> start, op.resetTag -> reset))
    }  
  }
  
  private def writeVar(id: String, value: SPValue) = {
    for {
      v <- varOpcDefs.values find (_.id == id)
    } yield {
      listener ! OPCWrite(Map(v.opcTag -> value))
    }   
  }
  
  private def updateOpcSubscription(opcDef: OpcDef) = {
    
    opStates = Map.empty

    import org.json4s._
    
    for {
    	opOpcDef <- opcDef.operations
    } yield {
      opStates += opOpcDef.id -> OpState(opOpcDef.id, JBool(false), JInt(0))
    }
    
    varStates = (for {
        varOpcDef <- opcDef.variables
      } yield varOpcDef.id -> VarState(varOpcDef.id, JNothing)) toMap
      
    opOpcDefs = (for {
        opOpcDef <- opcDef.operations
        kv <- Seq(opOpcDef.preTrueTag->opOpcDef, opOpcDef.stateTag->opOpcDef, opOpcDef.startTag->opOpcDef, opOpcDef.resetTag->opOpcDef)
      } yield kv) toMap

    varOpcDefs = (for {
          v <- opcDef.variables
        } yield v.opcTag -> v) toMap

    listener ! OPCSubscribe((opOpcDefs.keys ++ varOpcDefs.keys).toList)
  }
  
}