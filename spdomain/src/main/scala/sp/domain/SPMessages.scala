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

  case object StatusRequest extends APISP
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

    // using the extra reads to enable x.to[SPError]
    implicit val apiSPR1: JSReads[SPError] = deriveReadISA[SPError]
    implicit val apiSPR2: JSReads[SPACK] = deriveReadISA[SPACK]
    implicit val apiSPR3: JSReads[SPDone] = deriveReadISA[SPDone]
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
  def getHeaderAs[T](implicit fjs: JSReads[T]): Try[T] = header.to[T]
  def getBodyAs[T](implicit fjs: JSReads[T]): Try[T] = body.to[T]
  def toJson: String = Json.stringify(Json.toJson(this)(SPMessage.messageFormat))

  /**
    * Merge the header and replaces the body of the message
    * @param h The extra fields to add to the header. In most cases use SPAttributes("from" -> "me", ...) and not SPHeader
    * @param b The new body (will not merge with the existing body)
    * @param fjt An implicit format that needs to be in scope
    * @param fjs An implicit format that needs to be in scope
    * @return An SPMessage. Will always work since if the implicit do not exist, it will not compile
    */
  def make[T, S](h: T, b: S)(implicit fjt: JSWrites[T], fjs: JSWrites[S]): SPMessage = {
    val updSP = SPMessage.make(h, b)
    SPMessage(header.deepMerge(updSP.header), updSP.body)
  }
  def makeJson[T, S](h: T, b: S)(implicit fjt: JSWrites[T], fjs: JSWrites[S]): String = this.make(h, b).toJson
}

object SPHeader {
  implicit val headerFormat: JSFormat[SPHeader] = Json.format[SPHeader]
}

object SPMessage {
  implicit val messageFormat: JSFormat[SPMessage] = Json.format[SPMessage]

  //def apply(h: AttributeWrapper, b: AttributeWrapper): SPMessage = make(h, b)

  /**
    * Creates a SPMessage based on a header and a body. The paramterers must have a implicit format
    * inscope to be become an Attribute wrapper (implicit val myFormat = Json.format[MyCaseClass])
    *
    * @param h A header as a case class where an implicit format exist (preferable SPHeader)
    * @param b A body defined as a case class where an implicit format exist
    */
  def make[T, S](h: T, b: S)(implicit fjt: JSWrites[T], fjs: JSWrites[S]): SPMessage = {
    val hR = SPValue(h)
    val bR = SPValue(b)
    val hAsO = Try{hR.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)
    val bAsO = Try{bR.asInstanceOf[JsObject]}.getOrElse(JsObject.empty)

    SPMessage(hAsO, bAsO)
  }
  def makeJson[T, S](h: T, b: S)(implicit fjt: JSWrites[T], fjs: JSWrites[S]): String = this.make(h, b).toJson

  def fromJson(json: String) = Try{Json.parse(json).as[SPMessage](SPMessage.messageFormat)}

}







