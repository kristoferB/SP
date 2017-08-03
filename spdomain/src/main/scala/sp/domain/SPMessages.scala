package sp.domain

import java.util

import play.api.libs.json._
import sp.domain.Logic._

import scala.util.Try



sealed trait APISP
object APISP {

  // topics
  val services = "services"
  val answers = "answers"
  val spevents = "spevents"
  val commands = "commands"
  val events = "events"



  case class SPError(message: String, attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPACK() extends APISP
  case class SPDone() extends APISP

  case class StatusRequest(attributes: SPAttributes = SPAttributes()) extends APISP
  case class StatusResponse(service: String, instanceID: Option[ID] = None, instanceName: String = "", tags: List[String] = List(), api: SPAttributes = SPAttributes(), version: Int = 1, attributes: SPAttributes = SPAttributes()) extends APISP


  object StatusResponse {
    def apply(attr: SPAttributes): StatusResponse = {
      val service = attr.getAs[String]("service").getOrElse("noName")
      val instanceName = attr.getAs[String]("instanceName").orElse(attr.getAs[String]("name")).getOrElse("")
      val id = attr.getAs[ID]("instanceID")
      val tags = attr.getAs[List[String]]("tags").getOrElse(List())
      val api = attr.getAs[SPAttributes]("api").getOrElse(SPAttributes())
      val version = attr.getAs[Int]("version").getOrElse(1)
      StatusResponse(service, id, instanceName, tags, api, version, attr)
    }
  }

    import julienrf.json.derived
    // using the extra reads to enable x.to[SPError] and SPValue(SPOK())
    implicit val apiSPR1: JSReads[SPError] = deriveReadISA[SPError]
    implicit val apiSPR2: JSReads[SPACK] = deriveReadISA[SPACK]
    implicit val apiSPR3: JSReads[SPDone] = deriveReadISA[SPDone]
    implicit val apiSPR4: JSReads[StatusRequest] = deriveReadISA[StatusRequest]
    implicit val apiSPR5: JSReads[StatusResponse] = deriveReadISA[StatusResponse]
    implicit val apispFormat: JSFormat[APISP] = deriveFormatISA[APISP]
}


case class SPHeader(from: String = "", // the name of the sender
                    to: String = "", // the name of the receiver, empty if to anyone
                    reqID: ID = ID.newID, // the id to use for replies
                    reply: SPValue = SPAttributes(), // A data structure that should be included in all replies to be used for matching
                    fromTags: List[String] = List(), // a list of tags to define things about the sender. For example where the sender is located
                    toTags: List[String] = List(), // a list of tags to define things about possible receivers
                    attributes: SPAttributes = SPAttributes() // to be used in some scenarios, where more info in the header is needed
                   )

case class SPMessage(header: SPAttributes, body: SPAttributes) {
  def getHeaderAs[T](implicit fjs: Reads[T]) = header.to[T]
  def getBodyAs[T](implicit fjs: Reads[T]) = body.to[T]
  def toJson = Json.stringify(Json.toJson(this)(SPMessage.messageFormat))

  def make(h: AttributeWrapper, b: AttributeWrapper): SPMessage = {
    val headerAsO = Try{header.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)
    val hAsO = Try{h.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)
    val bodyAsO = Try{body.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)
    val bAsO = Try{b.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)
    SPMessage(headerAsO.deepMerge(hAsO), bodyAsO.deepMerge(bAsO))
  }
  def makeJson(h: AttributeWrapper, b: AttributeWrapper): String = this.make(h, b).toJson
}

object SPHeader {
  implicit val headerFormat = Json.format[SPHeader]
}

object SPMessage {
  implicit val messageFormat = Json.format[SPMessage]

  //def apply(h: AttributeWrapper, b: AttributeWrapper): SPMessage = make(h, b)

  /**
    * Creates a SPMessage based on a header and a body. The paramterers must have a implicit format
    * inscope to be become an Attribute wrapper (implicit val myFormat = Json.format[MyCaseClass])
    *
    * @param h A header as a case class where an implicit format exist (preferable SPHeader)
    * @param b A body defined as a case class where an implicit format exist
    */
  def make(h: AttributeWrapper, b: AttributeWrapper): SPMessage = {
    val hAsO = Try{h.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)
    val bAsO = Try{b.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)

    SPMessage(hAsO, bAsO)
  }
  def makeJson(h: AttributeWrapper, b: AttributeWrapper): String = make(h, b).toJson
  def fromJson(json: String) = Try{Json.parse(json).as[SPMessage](SPMessage.messageFormat)}

}






