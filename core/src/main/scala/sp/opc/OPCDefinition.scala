package sp.opc

import akka.util.Timeout
import sp.domain.{Thing, Operation, ID}
import sp.system.messages.{GetThings, SPIDs, GetOperations}
import akka.pattern.ask
import simpleJsonMessToWeb._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeUnit
import sp.system.SPActorSystem._

object OPCDefinition {

	private implicit val to = Timeout(20, TimeUnit.SECONDS)

	// opcServerPrefix: prefix common to all OPC variables, reflecting the OPC server name. In PSL it's "S7:[S7_Connection_1]".
	def get(model: ID, opcServerPrefix: String): OpcDef = {

		val opsF: Future[List[Operation]] = (modelHandler ? GetOperations(model)).mapTo[SPIDs] map(_.items.map(_.asInstanceOf[Operation]))
		val thingsF: Future[List[Thing]] = (modelHandler ? GetThings(model)).mapTo[SPIDs] map(_.items.map(_.asInstanceOf[Thing]))

		var opcOpDefs: List[OpOpcDef] = List()
		var opcVarDefs: List[VarOpcDef] = List()

		for {
			ops <- opsF
			things <- thingsF
		} yield {
			opcOpDefs = ops.map(op => constructOPopcDef(op)).flatten
			opcVarDefs = things.map(sv => constructVaropcDef(sv)).flatten
		}

		def constructVaropcDef(sv: Thing): Option[VarOpcDef] = {
			sv.attributes.getAsString("opcTag") match {
				case Some("") | None => None
				case Some(opcTag: String) => Some(VarOpcDef(JsID(sv.id.toString()), opcServerPrefix + opcTag))
			}
		}

		def constructOPopcDef(op: Operation): Option[OpOpcDef] = {
			op.attributes.getAsString("opcTag") match {
				case Some("") | None => None
				case Some(opcTag: String) =>
					val opVarBody = opcServerPrefix + opcTag
					Some(OpOpcDef(JsID(op.id.toString()), opVarBody + ".preTrue", opVarBody + ".state", opVarBody + ".start", opVarBody + ".reset"))
			}
		}

    OpcDef(opcOpDefs, opcVarDefs)

  }

}