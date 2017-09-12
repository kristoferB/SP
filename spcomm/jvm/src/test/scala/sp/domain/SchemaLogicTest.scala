package sp.domain

import org.scalatest._
import sp.domain.Logic._



object TAPI {
  sealed trait API

  //case class Connect(url: String) extends API
  case class P(p: IDAble) extends API
  case object Disconnect extends API
  case object GetNodes extends API
  //case class Subscribe(nodeIDs: List[String]) extends API
  case class Write(node: String, value: SPValue) extends API
  case class WriteAttr(value: SPAttributes) extends API

  // answers
  //case class ConnectionStatus(connected: Boolean) extends API
  case class AvailableNodes(nodes: Map[String, String]) extends API
  ///case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends API



  object API {
    implicit lazy val fMyAPIRequest: JSFormat[API] = deriveFormatISA[API]
  }



}


class SchemaLogicTest extends FreeSpec {
  "Creating schema" - {
    "construct for spdomain" in {

    import TestingSchema._

    lazy val s = com.sksamuel.avro4s.SchemaFor[API]()
      println(s)
    }
  }

}

object TestingSchema {
  import com.sksamuel.avro4s._
  import org.apache.avro.Schema

  implicit object SPValueToSchema extends ToSchema[SPValue] {
    override val schema: Schema = Schema.createRecord("SPValue", "An SPValue in the form of any Json structure", "sp.domain", false)
  }
  implicit object SPAttributesToSchema extends ToSchema[SPAttributes] {
    override val schema: Schema = Schema.createRecord("SPAttributes", "An SPAttributes in the form of Json objects", "sp.domain", false)
  }
  implicit object PropositionToSchema extends ToSchema[Proposition] {
    override val schema: Schema = Schema.createRecord("Proposition", "SP Proposition. Schema to come. For now, check case classes in SP source", "sp.domain", false)
  }
  implicit object StateUpdaterToSchema extends ToSchema[StateUpdater] {
    override val schema: Schema = Schema.createRecord("StateUpdater", "SP StateUpdater (action wrapper). Schema to come. For now, check case classes in SP source", "sp.domain", false)
  }
  implicit object SOPToSchema extends ToSchema[SOP] {
    override val schema: Schema = Schema.createRecord("SOP", "SP Sequences of Operation. Schema to come. For now, check case classes in SP source", "sp.domain", false)
  }


  //implicit val stateEvaluator

//  implicit object SPAttributesToSchema extends ToSchema[SPAttributes] {
//    override val schema: Schema = Schema.createRecord("SPAttributes", "An SPAttributes in the form of Json objects", "sp.domain", false)
//  }


  case class API(r: TAPI.API)

}

