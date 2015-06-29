package sp.opc

import akka.actor.ActorRef
import akka.util.Timeout
import sp.domain.{IDAble, Thing, Operation, ID}
import sp.system.messages._
import akka.pattern.ask
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import sp.system.SPActorSystem._

object OPCDefinition {

  private implicit val to = Timeout(20, TimeUnit.SECONDS)

  // opcServerPrefix: prefix common to all OPC variables, reflecting the OPC server name. In PSL it's "S7:[S7_Connection_1]".
  def get(model: ID, opcServerPrefix: String, idsToTags: Map[Option[ID], Option[String]], reply: ActorRef): OpcDef = {

    var opcOpDefinitions: List[OpOpcDef] = List()
    var opcVarDefinitions: List[VarOpcDef] = List()

    idsToTags foreach {
      case (Some(id: ID), Some(tag: String)) =>
        val opVarBody = opcServerPrefix + tag
        val idAble: Any = Await.result(modelHandler ? GetIds(model, List(id)), to.duration)
        idAble match {
          case SPIDs(List(_: Thing)) =>
            opcVarDefinitions = VarOpcDef(id.toString(), opcServerPrefix + tag) :: opcVarDefinitions
          case SPIDs(List(_: Operation)) =>
            opcOpDefinitions = OpOpcDef(id.toString(), opVarBody + "_preTrue", opVarBody + "_state", opVarBody + "_start", opVarBody + "_reset") :: opcOpDefinitions
          case MissingID =>
            reply ! SPError("The ID " + id.toString() + " is not present in the model.")
          case SPIDs(List(item: IDAble)) =>
            reply ! SPError("Item " + item.name + " is not an Operation nor a Thing.")
          case _ =>
            reply ! SPError("There\'s something strange with ID " + id.toString())
        }
      case _ => reply ! SPError("Erroneous or missing id or opcTag in this OPCDef.")
    }

    OpcDef(opcOpDefinitions, opcVarDefinitions)

  }

}