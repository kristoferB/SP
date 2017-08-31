package sp.domain

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
  implicit object IDToSchema extends ToSchema[java.util.UUID] {
    override val schema: Schema = Schema.createRecord("ID", "An SP ID as an UUID", "sp.domain", false)
  }


  implicit object OperationToSchema extends ToSchema[Operation] {
    override val schema: Schema = AvroSchema[Operation]
  }
  implicit object ThingToSchema extends ToSchema[Thing] {
    override val schema: Schema = AvroSchema[Thing]
  }
  implicit object SOPSpecToSchema extends ToSchema[SOPSpec] {
    override val schema: Schema = AvroSchema[SOPSpec]
  }
  implicit object SPSpecToSchema extends ToSchema[SPSpec] {
    override val schema: Schema = AvroSchema[SPSpec]
  }
  implicit object SPResultToSchema extends ToSchema[SPResult] {
    override val schema: Schema = AvroSchema[SPResult]
  }
  implicit object SPStateToSchema extends ToSchema[SPState] {
    override val schema: Schema = AvroSchema[SPState]
  }
  implicit object StructToSchema extends ToSchema[Struct] {
    override val schema: Schema = AvroSchema[Struct]
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






