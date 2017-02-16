package sp.domain

import java.util.UUID


case class SPMessage(header: SPValue, body: SPValue)
case class SPHeader(from: String,
                    to: String,
                    replyTo: String,
                    reqID: UUID = UUID.randomUUID(),
                    replyFrom: String = "",
                    replyID: Option[UUID] = None)



sealed trait APISP
object APISP {
  case class SPError(message: String, attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPACK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPOK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPDone(attributes: SPAttributes = SPAttributes()) extends APISP

  case class StatusRequest(attributes: SPAttributes = SPAttributes()) extends APISP
  case class StatusResponse(attributes: SPAttributes = SPAttributes()) extends APISP

//  implicit val readWriter: ReadWriter[APISP] =
//    macroRW[SPError] merge macroRW[SPACK] merge macroRW[SPOK] merge macroRW[SPDone] merge macroRW[StatusRequest] merge macroRW[StatusResponse]
}