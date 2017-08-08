package sp.domain.logic

import org.threeten.bp._
import sp.domain._
import play.api.libs.json._
import julienrf.json.derived

object SchemaLogic extends SchemaImplicit

trait SchemaImplicit {
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


  def makeMeASchema(request: Schema, response: Schema) = {
    (for {
      req <- SPAttributes.fromJson(request.toString)
      resp <- SPAttributes.fromJson(response.toString)
    } yield {
      SPAttributes("request" -> req, "response"->resp)
    } ).getOrElse(SPAttributes("error" -> "Schema creation error"))
  }


}






